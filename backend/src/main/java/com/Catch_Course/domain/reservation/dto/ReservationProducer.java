package com.Catch_Course.domain.reservation.dto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReservationProducer {

    private final KafkaTemplate<String, ReservationRequest> kafkaTemplate;
    private static final String TOPIC = "course-reservation";

    public void send(ReservationRequest reservationRequest) {
        log.info("Sending reservation request: {}", reservationRequest);
        kafkaTemplate.send(TOPIC, reservationRequest);
    }
}