package com.Catch_Course.domain.payments.service;

import com.Catch_Course.domain.payments.entity.PaymentOutbox;
import com.Catch_Course.domain.payments.repository.PaymentOutboxRepository;
import com.Catch_Course.global.kafka.dto.PaymentCancelRequest;
import com.Catch_Course.global.kafka.producer.PaymentCancelProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private final PaymentOutboxRepository paymentOutboxRepository;
    private final PaymentCancelProducer paymentCancelProducer;
    private final ObjectMapper objectMapper;

    // 10초마다 실행 (fixedDelayString = "10000")
    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void pollAndPublish() {
        List<PaymentOutbox> events = paymentOutboxRepository.findTop100ByOrderByCreatedAtAsc();
        if (events.isEmpty()) {
            return;
        }

        log.info("[Outbox] 미처리 이벤트 {}건을 조회했습니다. 메시지 발행을 시작합니다.", events.size());

        for (PaymentOutbox event : events) {
            try {
                // 2. 페이로드를 역직렬화
                PaymentCancelRequest payload = objectMapper.readValue(event.getPayload(), PaymentCancelRequest.class);

                // 3. 메시지 발행
                paymentCancelProducer.send(payload);

                // 4. 아웃박스 테이블에서 삭제
                paymentOutboxRepository.delete(event);

            } catch (Exception e) {
                log.error("[Outbox] 이벤트 처리 실패. eventId: {}. 에러: {}", event.getId(), e.getMessage());
            }
        }
    }
}