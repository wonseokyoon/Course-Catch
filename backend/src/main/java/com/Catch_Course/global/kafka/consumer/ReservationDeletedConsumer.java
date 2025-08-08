package com.Catch_Course.global.kafka.consumer;

import com.Catch_Course.domain.reservation.service.ReservationService;
import com.Catch_Course.global.kafka.dto.ReservationDeletedRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationDeletedConsumer {

    private final ReservationService reservationService;

    // 구독
    @KafkaListener(topics = "course-reservation-deleted", groupId = "course")
    public void consume(ReservationDeletedRequest reservationDeletedRequest) {

        log.info("Consuming reservation request: {}", reservationDeletedRequest);
        try {
            // 실제 로직 호출
            log.info("{}의 수강 취소 이력이 성공적으로 처리되었습니다.", reservationDeletedRequest.getCourseId());
        } catch (Exception e){
            log.error("Error Request: {}", reservationDeletedRequest, e);
        }

    }
}