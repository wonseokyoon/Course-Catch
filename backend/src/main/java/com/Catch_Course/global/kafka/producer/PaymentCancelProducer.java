package com.Catch_Course.global.kafka.producer;

import com.Catch_Course.global.kafka.dto.PaymentCancelRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentCancelProducer {

    private final KafkaTemplate<String, PaymentCancelRequest> kafkaTemplate;
    private static final String TOPIC = "payment_cancel";

    public void send(PaymentCancelRequest paymentCancelRequest) {
        String key = String.valueOf(paymentCancelRequest.getPaymentId());
        log.info("결제 취소 요청 메세지 전송: {}", paymentCancelRequest);
        kafkaTemplate.send(TOPIC, key, paymentCancelRequest);
    }
}