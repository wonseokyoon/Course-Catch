package com.Catch_Course.global.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationCancelRequest {
    private Long reservationId;
    private Long memberId;
    private Long courseId;
}
