package com.Catch_Course.domain.reservation.repository;

import com.Catch_Course.domain.course.entity.Course;
import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.reservation.entity.Reservation;
import com.Catch_Course.domain.reservation.entity.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    @EntityGraph(attributePaths = {"course", "student"})
    Optional<Reservation> findByStudentAndCourse(Member member, Course course);

    @EntityGraph(attributePaths = {"course", "student"})
    Page<Reservation> findAllByStudentAndStatus(Member member, ReservationStatus reservationStatus, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"course", "student", "payment"})
    Optional<Reservation> findByIdAndStudentAndStatusWithPessimisticLock(Long reservationId, Member member, ReservationStatus reservationStatus);

    @EntityGraph(attributePaths = {"course", "student", "payment"})
    Optional<Reservation> findByIdAndStudentAndStatus(Long reservationId, Member member, ReservationStatus reservationStatus);

    // Reservation 조회 시 연관된 Course도 함께 가져오는 메서드
    @Query("SELECT r FROM Reservation r JOIN FETCH r.course WHERE r.id = :id")
    Optional<Reservation> findWithCourseById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)   // 비관적 Lock, FOR UPDATE 전부 차단
    @Query("select r from Reservation r where r.id= :id")
    Optional<Reservation> findByIdWithPessimisticLock(@Param("id") Long id);

}
