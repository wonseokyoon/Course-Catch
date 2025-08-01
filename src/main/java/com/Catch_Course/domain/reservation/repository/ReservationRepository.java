package com.Catch_Course.domain.reservation.repository;

import com.Catch_Course.domain.course.entity.Course;
import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.reservation.entity.Reservation;
import com.Catch_Course.domain.reservation.entity.ReservationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    @EntityGraph(attributePaths = {"course","student"})  // course 와 student 필드를 즉시 로딩
    Optional<Reservation> findByStudentAndCourse(Member member, Course course);

    @EntityGraph(attributePaths = {"course","student"})  // course 와 student 필드를 즉시 로딩
    List<Reservation> findAllByStudentAndStatus(Member member, ReservationStatus reservationStatus);
}
