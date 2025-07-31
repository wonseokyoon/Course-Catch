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
                .status(ReservationStatus.COMPLETE)
                .build();

        return reservationRepository.save(reservation);
    }
}
