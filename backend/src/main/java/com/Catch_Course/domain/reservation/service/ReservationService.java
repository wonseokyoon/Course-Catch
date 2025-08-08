package com.Catch_Course.domain.reservation.service;

import com.Catch_Course.domain.course.entity.Course;
import com.Catch_Course.domain.course.repository.CourseRepository;
import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.member.repository.MemberRepository;
import com.Catch_Course.domain.reservation.entity.Reservation;
import com.Catch_Course.domain.reservation.entity.ReservationStatus;
import com.Catch_Course.domain.reservation.repository.ReservationRepository;
import com.Catch_Course.global.exception.ServiceException;
import com.Catch_Course.global.kafka.dto.ReservationDeletedRequest;
import com.Catch_Course.global.kafka.dto.ReservationRequest;
import com.Catch_Course.global.kafka.producer.ReservationDeletedProducer;
import com.Catch_Course.global.kafka.producer.ReservationProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationService {

    private final CourseRepository courseRepository;
    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;
    private final ReservationProducer reservationProducer;
    private final ReservationDeletedProducer reservationDeletedProducer;

    public Reservation addToQueue(Member member, Long courseId) {
        Course course = courseRepository.findByIdWithPessimisticLock(courseId)  // 비관적 Lock 을 걸고 조회
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 강의입니다."));

        Optional<Reservation> optionalReservation = reservationRepository.findByStudentAndCourse(member, course);
        if (optionalReservation.isPresent()) {

            // 중복 신청
            if(!isCanceled(optionalReservation.get())){
                handleDuplicateReservation(optionalReservation.get(),member.getId(),courseId);
                return optionalReservation.get();
            }

            // 삭제
            reservationRepository.delete(optionalReservation.get());
        }

        // 대기열 등록
        Reservation reservation = Reservation.builder()
                        .student(member)
                        .course(course)
                        .status(ReservationStatus.WAITING)
                        .build();

        // 메세지 전송
        reservationProducer.send(new ReservationRequest(member.getId(), courseId));

        return reservationRepository.save(reservation);
    }

    private boolean isCanceled(Reservation reservation) {
        ReservationStatus status = reservation.getStatus();
        return status == ReservationStatus.CANCELED;
    }

    private void handleDuplicateReservation(Reservation reservation,Long memberId,Long courseId) {
        ReservationStatus status = reservation.getStatus();

        if (status.equals(ReservationStatus.COMPLETED)) {
            throw new ServiceException("409-1", "이미 신청한 강의입니다.");
        } else if (status.equals(ReservationStatus.WAITING)) {
            throw new ServiceException("409-3", "이미 대기열에 등록된 신청입니다.");
        }
    }

    public Reservation cancelReserve(Member member, Long courseId) {

        Course course = courseRepository.findByIdWithPessimisticLock(courseId)  // 잠금
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 강의입니다."));

        Reservation reservation = reservationRepository.findByStudentAndCourse(member, course)
                .orElseThrow(() -> new ServiceException("404-3", "수강 신청 이력이 없습니다."));

        if (reservation.getStatus() == ReservationStatus.CANCELED) {
            throw new ServiceException("409-2", "이미 취소된 수강 신청입니다.");
        }

        course.decreaseReservation();
        courseRepository.save(course);

        reservation.setStatus(ReservationStatus.CANCELED);    // 상태 관리로 삭제 처리
//        reservationRepository.delete(reservation);        // 나중에 필요하면 삭제 처리

        // 수강 취소는 메세지를 남겨서 저장
        reservationDeletedProducer.send(new ReservationDeletedRequest(member.getId(), courseId));
        return reservationRepository.save(reservation);
    }

    @Transactional(readOnly = true)
    public Page<Reservation> getReservations(Member member, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);

        Page<Reservation> reservations = reservationRepository.findAllByStudentAndStatus(member, ReservationStatus.COMPLETED,pageable);

        if (reservations.isEmpty()) {
            throw new ServiceException("404-3", "수강신청 이력이 없습니다.");
        }

        return reservations;
    }

    /**
     * Kafka Consumer에 의해 호출 될 실제 수강 신청 처리 메서드
     */
    public void processReservation(Long courseId, Long memberId) {
        // 락을 걸어 강의 정보 조회(동시성 제어)
        Course course = courseRepository.findByIdWithPessimisticLock(courseId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 강의입니다."));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException("404-4", "회원을 찾을 수 없습니다."));

        Reservation reservation = reservationRepository.findByStudentAndCourse(member, course)
                .orElseThrow(() -> new ServiceException("404-3", "수강신청 이력이 없습니다."));

        if(course.isFull()){
            reservation.setStatus(ReservationStatus.FAILED);
            reservationRepository.save(reservation);
            throw new ServiceException("406-1","남은 좌석이 없습니다.");
        }

        // 수강 신청 처리
        course.increaseReservation();
        courseRepository.save(course);

        reservation.setStatus(ReservationStatus.COMPLETED);
        reservationRepository.save(reservation);
    }
}
