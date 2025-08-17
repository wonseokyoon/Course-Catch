package com.Catch_Course.global.kafka.producer;

import com.Catch_Course.global.kafka.dto.ReservationRequest;
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

    public void send(String key, ReservationRequest reservationRequest) {
        log.info("수강신청 요청 메세지 전송: {}", reservationRequest);
        kafkaTemplate.send(TOPIC,key, reservationRequest);
    }
}