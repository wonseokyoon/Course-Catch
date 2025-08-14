package com.Catch_Course.domain.reservation.controller;

import com.Catch_Course.domain.course.entity.Course;
import com.Catch_Course.domain.course.repository.CourseRepository;
import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.reservation.entity.Reservation;
import com.Catch_Course.domain.reservation.entity.ReservationStatus;
import com.Catch_Course.domain.reservation.repository.ReservationRepository;
import com.Catch_Course.domain.reservation.service.ReservationService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReservationTestHelper {
    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;
    private final CourseRepository courseRepository;
    private final EntityManager em;

    @Transactional(propagation = Propagation.REQUIRES_NEW)  // 반드시 새로운 트랜잭션 생성
    public void reserveSetUp(Member loginedMember, Course course) {
        Reservation reservation = new Reservation(loginedMember,course, ReservationStatus.WAITING, course.getPrice());  // 대기열 등록
        reservationRepository.save(reservation);
        reservationService.processReservation(course.getId(), loginedMember.getId());    // 수강 신청
        em.flush(); // DB에 적고
        em.clear(); // 영속성 컨텍스트 클리어
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)  // 반드시 새로운 트랜잭션 생성
    public void currentRegistrationSetUp(Long courseId) {
        Course course = courseRepository.findByIdWithPessimisticLock(courseId).get();
        course.setCurrentRegistration(course.getCapacity());
        em.flush(); // DB에 적고
        em.clear(); // 영속성 컨텍스트 클리어
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)  // 반드시 새로운 트랜잭션 생성
    public void addQueue(Member loginedMember,Long courseId) {
        reservationService.addToQueue(loginedMember,courseId);
        em.flush(); // DB에 적고
        em.clear(); // 영속성 컨텍스트 클리어
    }
}
