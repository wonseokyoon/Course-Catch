package com.Catch_Course.global.scheduler;


import com.Catch_Course.domain.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationExpirationScheduler {

    private final ReservationService reservationService;

    // 1분마다 실행 (cron = "초 분 시 일 월 요일")
    @Scheduled(cron = "0 30 14 * * *")
    public void expirePendingPayments() {
        log.info("결제 대기 시간 만료 스케줄러 시작");
        reservationService.expireOldPendingPayments();
        log.info("결제 대기 시간 만료 스케줄러 종료");
    }
}
