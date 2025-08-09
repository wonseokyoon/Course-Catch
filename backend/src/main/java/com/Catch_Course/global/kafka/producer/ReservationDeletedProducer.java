package com.Catch_Course.global.kafka.producer;

import com.Catch_Course.global.kafka.dto.ReservationDeletedRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReservationDeletedProducer {

    // 수강 취소하는 이력 메세지로 저장하여 관리

    private final KafkaTemplate<String, ReservationDeletedRequest> kafkaTemplate;
    private static final String TOPIC = "course-reservation-deleted";

    public void send(ReservationDeletedRequest request) {
        log.info("Sending reservation deleted request: {}", request);

        kafkaTemplate.send(TOPIC, request);
    }
}
