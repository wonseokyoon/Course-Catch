package com.Catch_Course.global.payment;

import com.Catch_Course.global.exception.ServiceException;
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
public class TossPaymentsService {

    private final String secretKey;
    private final RestTemplate restTemplate;
    private static final String TOSS_CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";

    public TossPaymentsService(@Value("${custom.toss.payment.secret}") String secretKey, RestTemplate restTemplate) {
        this.secretKey = secretKey;
        this.restTemplate = restTemplate;
    }

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
            // [추가] API 호출 직전, 실제 전송되는 헤더 값을 로그로 확인합니다.
            log.info("전송될 Authorization 헤더: {}", headers.getFirst("Authorization"));

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
        // Base64 인코딩 시, 생성자에서 주입받은 final secretKey를 사용합니다.
        String encodedKey = Base64.getEncoder().encodeToString((this.secretKey.trim() + ":").getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }
}
