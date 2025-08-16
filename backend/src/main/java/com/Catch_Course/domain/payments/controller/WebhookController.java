package com.Catch_Course.domain.payments.controller;

import com.Catch_Course.domain.payments.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final PaymentService paymentService;

    // Toss Payments가 보내주는 Webhook 요청을 처리하는 엔드포인트
    @PostMapping("/toss")
    public ResponseEntity<Void> handleTossWebhook(@RequestBody Map<String, Object> payload) {
        log.info("Toss Payments 웹훅 수신: {}", payload);

        // Webhook 페이로드에서 이벤트 타입과 주문 ID 추출
        String eventType = (String) payload.get("eventType");
        Map<String, Object> data = (Map<String, Object>) payload.get("data");
        String orderId = (String) data.get("orderId");

        // "결제 성공" 이벤트일 때만 처리
        if ("PAYMENT_CONFIRMED".equalsIgnoreCase(eventType)) {
            paymentService.syncPaymentStatus(orderId);
        }

        // PG사에게 정상적으로 수신했음을 알림 (HTTP 200 OK)
        return ResponseEntity.ok().build();
    }
}
