package com.Catch_Course.global.payment;

import com.Catch_Course.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TossPaymentsService {
    @Value("${custom.toss.payment.secret}")
    private String secretKey;
    private final RestTemplate restTemplate;
    private final String TOSS_CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";

    public void confirm(String paymentKey, String orderId, Long amount) {
        log.info("Toss Payments 결제 승인 요청 시작: orderId={}", orderId);

        HttpHeaders headers = createHeaders();
        Map<String, Object> requestBody = Map.of(
                "paymentKey", paymentKey,
                "orderId", orderId,
                "amount", amount
        );

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            // API 호출
            restTemplate.postForObject(TOSS_CONFIRM_URL, requestEntity, Map.class);
            log.info("Toss Payments 결제 승인 성공: orderId={}", orderId);
        } catch (Exception e) {
            log.error("Toss Payments API 연동 실패: {}", e.getMessage());
            throw new ServiceException("400-5", "결제 승인 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8)));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }
}
