package com.Catch_Course.global.kafka.consumer;

import com.Catch_Course.domain.reservation.service.ReservationService;
import com.Catch_Course.global.kafka.dto.ReservationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationConsumer {

    private final ReservationService reservationService;

    // 구독
    @KafkaListener(topics = "course-reservation", groupId = "course")
    @Transactional
    public void consume(ReservationRequest reservationRequest) {
        log.info("수강신청 처리 시작 : {}", reservationRequest);
        reservationService.processReservation(reservationRequest.getCourseId(),reservationRequest.getMemberId());
    }
}