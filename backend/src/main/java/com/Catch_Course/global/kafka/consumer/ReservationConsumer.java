package com.Catch_Course.global.kafka.consumer;

import com.Catch_Course.domain.reservation.service.ReservationService;
import com.Catch_Course.global.kafka.dto.ReservationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationConsumer {

    private final ReservationService reservationService;

    // 구독
    @KafkaListener(topics = "course-reservation", groupId = "course")
    public void consume(ReservationRequest reservationRequest) {

        log.info("Consuming reservation request: {}", reservationRequest);
        try {
            // 실제 로직 호출
            reservationService.processReservation(reservationRequest.getCourseId(),reservationRequest.getMemberId());
        } catch (Exception e){
            log.error("Error Request: {}", reservationRequest, e);
        }

    }
}