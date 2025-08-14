package com.Catch_Course.domain.payments.entity;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.reservation.entity.Reservation;
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
public class Payment extends BaseTime {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(unique = true, nullable = false)
    private String merchantUid;     // 가맹점 주문번호(toss payments orderId)

    @Column(nullable = false)
    private long amount;        // 금액

    private String paymentKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

}
