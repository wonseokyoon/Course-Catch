package com.Catch_Course.domain.payments.entity;

import com.Catch_Course.global.entity.BaseTime;
import jakarta.persistence.Entity;
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
public class CancelHistory extends BaseTime {
    private Long paymentId;
    private Long reservationId;
    private String orderId;
    private String memberNickname;
    private String courseTitle;
    private Long amount;
}


