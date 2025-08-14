package com.Catch_Course.domain.payments.dto;

import com.Catch_Course.domain.payments.entity.Payment;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PaymentDto {
    private long reservationId;
    private String courseTitle;
    private String instructor;
    private String orderId;
    private long amount;
    private String paymentKey;
    private String status;
    @JsonProperty("createdDatetime")
    private LocalDateTime createdDate;

    public PaymentDto(Payment payment) {
        this.reservationId = payment.getReservation().getId();
        this.courseTitle = payment.getReservation().getCourse().getTitle();
        this.instructor = payment.getReservation().getCourse().getInstructor().getNickname();
        this.orderId = payment.getMerchantUid();
        this.amount = payment.getAmount();
        this.paymentKey = payment.getPaymentKey();
        this.status = payment.getStatus().toString();
        this.createdDate = payment.getCreatedDate();
    }
}
