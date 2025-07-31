package com.Catch_Course.domain.reservation.service;

import com.Catch_Course.domain.course.entity.Course;
import com.Catch_Course.domain.course.repository.CourseRepository;
import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.reservation.entity.Reservation;
import com.Catch_Course.domain.reservation.entity.ReservationStatus;
import com.Catch_Course.domain.reservation.repository.ReservationRepository;
import com.Catch_Course.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final CourseRepository courseRepository;
    private final ReservationRepository reservationRepository;


    public Reservation reserve(Member member, Long courseId) {

        Course course = courseRepository.findByIdWithPessimisticLock(courseId)  // 비관적 Lock 을 걸고 조회
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 강의입니다."));

        Optional<Reservation> optionalReservation = reservationRepository.findByStudentAndCourse(member, course);

        if (optionalReservation.isPresent()) {
            // 신청 내역이 존재하는 경우 처리 메서드
            return handleExistingReservation(optionalReservation.get(),course);
        }

        course.increaseReservation();
        courseRepository.save(course);

        Reservation reservation = Reservation.builder()
                .student(member)
                .course(course)
                .status(ReservationStatus.COMPLETED)
                .build();

        return reservationRepository.save(reservation);
    }

    private Reservation handleExistingReservation(Reservation reservation,Course course) {

        if(reservation.getStatus().equals(ReservationStatus.COMPLETED)){
            throw new ServiceException("409-1","이미 신청한 강의입니다.");
        } else if (reservation.getStatus().equals(ReservationStatus.WAITING)) {
            // todo 대기열 참가
        }

        course.increaseReservation();
        courseRepository.save(course);
        reservation.setStatus(ReservationStatus.COMPLETED);   // 상태 변경

        return reservationRepository.save(reservation);
    }

    public Reservation cancelReserve(Member member, Long courseId) {

        Course course = courseRepository.findByIdWithPessimisticLock(courseId)  // 잠금
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 강의입니다."));

        Reservation reservation = reservationRepository.findByStudentAndCourse(member, course)
                .orElseThrow(() -> new ServiceException("404-3","수강 신청 이력이 없습니다."));

        if (reservation.getStatus() == ReservationStatus.CANCELED) {
            throw new ServiceException("409-2", "이미 취소된 수강 신청입니다.");
        }

        course.decreaseReservation();
        courseRepository.save(course);

        reservation.setStatus(ReservationStatus.CANCELED);    // 상태 관리로 삭제 처리
//        reservationRepository.delete(reservation);        // 나중에 필요하면 삭제 처리

        return reservationRepository.save(reservation);
    }
}
