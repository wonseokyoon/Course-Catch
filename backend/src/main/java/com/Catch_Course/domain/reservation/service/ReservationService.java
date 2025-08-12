package com.Catch_Course.domain.reservation.service;

import com.Catch_Course.domain.course.entity.Course;
import com.Catch_Course.domain.course.repository.CourseRepository;
import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.member.repository.MemberRepository;
import com.Catch_Course.domain.reservation.dto.ReservationDto;
import com.Catch_Course.domain.notification.dto.NotificationDto;
import com.Catch_Course.domain.reservation.entity.DeletedHistory;
import com.Catch_Course.domain.reservation.entity.Reservation;
import com.Catch_Course.domain.reservation.entity.ReservationStatus;
import com.Catch_Course.domain.reservation.repository.DeleteHistoryRepository;
import com.Catch_Course.domain.reservation.repository.ReservationRepository;
import com.Catch_Course.global.exception.ServiceException;
import com.Catch_Course.global.kafka.dto.ReservationDeletedRequest;
import com.Catch_Course.global.kafka.dto.ReservationRequest;
import com.Catch_Course.global.kafka.producer.ReservationDeletedProducer;
import com.Catch_Course.global.kafka.producer.ReservationProducer;
import com.Catch_Course.global.sse.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReservationService {

    private final CourseRepository courseRepository;
    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;
    private final ReservationProducer reservationProducer;
    private final ReservationDeletedProducer reservationDeletedProducer;
    private final DeleteHistoryRepository deleteHistoryRepository;
    private final SseService sseService;

    public Reservation addToQueue(Member member, Long courseId) {
        Course course = courseRepository.findByIdWithPessimisticLock(courseId)  // 비관적 Lock 을 걸고 조회
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 강의입니다."));

        Optional<Reservation> optionalReservation = reservationRepository.findByStudentAndCourse(member, course);
        if (optionalReservation.isPresent()) {
            handleDuplicateReservation(optionalReservation.get());
            return optionalReservation.get();
        }

        // 대기열 등록
        Reservation reservation = Reservation.builder()
                .student(member)
                .course(course)
                .status(ReservationStatus.WAITING)
                .build();

        // 메세지 전송
        reservationProducer.send(String.valueOf(courseId), new ReservationRequest(member.getId(), courseId));
        return reservationRepository.save(reservation);
    }

    private void handleDuplicateReservation(Reservation reservation) {
        ReservationStatus status = reservation.getStatus();

        if (status.equals(ReservationStatus.COMPLETED)) {
            throw new ServiceException("409-1", "이미 신청한 강의입니다.");
        } else if (status.equals(ReservationStatus.WAITING)) {
            throw new ServiceException("409-3", "이미 대기열에 등록된 신청입니다.");
        }
    }

    public ReservationDto cancelReserve(Member member, Long courseId) {

        Course course = courseRepository.findByIdWithPessimisticLock(courseId)  // 잠금
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 강의입니다."));

        Reservation reservation = reservationRepository.findByStudentAndCourse(member, course)
                .orElseThrow(() -> new ServiceException("404-3", "수강 신청 이력이 없습니다."));

        ReservationDto reservationDto = new ReservationDto(reservation);

        course.decreaseReservation();
        courseRepository.save(course);

        // 수강 취소는 메세지를 남겨서 저장
        reservationDeletedProducer.send(new ReservationDeletedRequest(member.getId(), courseId));
        reservationRepository.delete(reservation);

        return reservationDto;
    }

    @Transactional(readOnly = true)
    public Page<Reservation> getReservations(Member member, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);

        Page<Reservation> reservations = reservationRepository.findAllByStudentAndStatus(member, ReservationStatus.COMPLETED, pageable);

        if (reservations.isEmpty()) {
            throw new ServiceException("404-3", "수강신청 이력이 없습니다.");
        }

        return reservations;
    }

    /**
     * Kafka Consumer에 의해 호출 될 실제 수강 신청 처리 메서드
     */
    public void processReservation(Long courseId, Long memberId) {
        try{
            // 락을 걸어 강의 정보 조회(동시성 제어)
            Course course = courseRepository.findByIdWithPessimisticLock(courseId)
                    .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 강의입니다."));

            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new ServiceException("404-4", "회원을 찾을 수 없습니다."));

            Reservation reservation = reservationRepository.findByStudentAndCourse(member, course)
                    .orElseThrow(() -> new ServiceException("404-3", "수강신청 이력이 없습니다."));

            if (course.isFull()) {
                reservation.setStatus(ReservationStatus.FAILED);
                reservationRepository.save(reservation);
                NotificationDto notificationDto = new NotificationDto(reservation,"수강 신청 실패: 정원이 마감되었습니다.");
                sseService.sendToClient(memberId,"ReservationResult", notificationDto);
                return;
            }

            // 수강 신청 처리
            course.increaseReservation();
            courseRepository.save(course);

            reservation.setStatus(ReservationStatus.COMPLETED);
            NotificationDto notificationDto = new NotificationDto(reservation,"수강 신청이 성공하였습니다.");
            sseService.sendToClient(memberId,"ReservationResult", notificationDto);
            reservationRepository.save(reservation);
        }catch (ServiceException e) {
            NotificationDto notificationDto = new NotificationDto(ReservationStatus.FAILED,"수강 신청 처리 중 오류가 발생했습니다. "+ e.getMessage());
            sseService.sendToClient(memberId,"ReservationResult", notificationDto);
        }
    }

    public void saveDeleteHistory(Long memberId, Long courseId) {
        // 기록으로 남김
        deleteHistoryRepository.save(DeletedHistory.builder()
                .memberId(memberId)
                .courseId(courseId)
                .build()
        );
    }
}
