package com.Catch_Course.domain.reservation.entity;

import com.Catch_Course.domain.course.entity.Course;
import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.payments.entity.Payment;
import com.Catch_Course.global.entity.BaseTime;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
// 중복 신청 방지
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student_id","course_id"})
})
public class Reservation extends BaseTime {

    @ManyToOne(fetch = FetchType.LAZY)
    private Member student;      // 신청 학생

    @ManyToOne(fetch = FetchType.LAZY)
    private Course course;      // 신청 강의

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;      // 신청 상태

    @OneToOne(mappedBy = "reservation", fetch = FetchType.LAZY)
    private Payment payment;

    @Column(nullable = false)
    private Long price;
}
