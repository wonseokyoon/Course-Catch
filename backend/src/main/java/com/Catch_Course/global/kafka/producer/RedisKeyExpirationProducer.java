package com.Catch_Course.global.kafka.producer;

import com.Catch_Course.global.kafka.dto.ReservationCancelRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RedisKeyExpirationProducer {

    private final KafkaTemplate<String, ReservationCancelRequest> kafkaTemplate;
    private static final String TOPIC = "course-reservation-expired";

    public void send(ReservationCancelRequest request) {
        log.info("결제 기한 만료로 인한 수강 취소 요청 메세지 전송: {}", request);
        kafkaTemplate.send(TOPIC, request);
    }
}
