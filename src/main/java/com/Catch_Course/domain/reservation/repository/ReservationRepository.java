package com.Catch_Course.domain.reservation.repository;

import com.Catch_Course.domain.course.entity.Course;
import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    boolean existsByStudentAndCourse(Member member, Course course);
}
