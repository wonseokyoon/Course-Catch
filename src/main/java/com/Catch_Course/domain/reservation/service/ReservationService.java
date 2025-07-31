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

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final CourseRepository courseRepository;
    private final ReservationRepository reservationRepository;


    public Reservation reserve(Member member, Long courseId) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 강의입니다."));

        if (reservationRepository.existsByStudentAndCourse(member, course)) {
            throw new ServiceException("409-1","이미 신청한 강의입니다.");
        }

        course.increaseReservation();   // 신청 인원 증가
        courseRepository.save(course);  // 저장

        Reservation reservation = Reservation.builder()
                .student(member)
                .course(course)
                .status(ReservationStatus.COMPLETED)
                .build();

        return reservationRepository.save(reservation);
    }

    public Reservation cancelReserve(Member member, Long courseId) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 강의입니다."));

        Reservation reservation = reservationRepository.findByStudentAndCourse(member, course)
                .orElseThrow(() -> new ServiceException("404-3","수강 신청 이력이 없습니다."));

        if (reservation.getStatus() == ReservationStatus.CANCELED) {
            throw new ServiceException("409-2", "이미 취소된 수강 신청입니다.");
        }

        course.decreaseReservation();   // 신청 인원 감소
        courseRepository.save(course);  // 저장

        reservation.setStatus(ReservationStatus.CANCELED);    // 상태 관리로 삭제 처리
//        reservationRepository.delete(reservation);        // 나중에 필요하면 삭제 처리

        return reservationRepository.save(reservation);
    }
}
