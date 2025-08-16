package com.Catch_Course.global.kafka.dto;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.payments.entity.Payment;
import com.Catch_Course.domain.reservation.entity.Reservation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCancelRequest {
    private Long paymentId;
    private String paymentKey;
    private String cancelReason;
    private Long reservationId;
    private Long memberId;
    private Long courseId;

    public PaymentCancelRequest(Payment payment, String cancelReason, Reservation reservation, Member member) {
        this.paymentId = payment.getId();
        this.paymentKey = payment.getPaymentKey();
        this.cancelReason = cancelReason;
        this.reservationId = reservation.getId();
        this.memberId = member.getId();
        this.courseId = reservation.getCourse().getId();
    }
}
