package com.Catch_Course.global.scheduler;

import com.Catch_Course.domain.reservation.service.ReservationService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ReservationExpirationScheduler {

    private final ReservationService reservationService;
    private final Timer reservationSchedulerTimer; // Timer 주입

    // MeterRegistry를 통해 Timer 빈을 생성
    public ReservationExpirationScheduler(ReservationService reservationService, MeterRegistry meterRegistry) {
        this.reservationService = reservationService;
        // "scheduler.reservation.expiration" 이라는 이름으로 Timer 메트릭 등록
        this.reservationSchedulerTimer = Timer.builder("scheduler.reservation.expiration")
                .description("결제 대기 시간 만료 스케줄러의 실행 시간 측정")
                .tag("scheduler", "reservation") // 검색 및 필터링을 위한 태그
                .register(meterRegistry);
    }


    @Scheduled(cron = "0 * * * * *")
    public void expirePendingPayments() {
        log.info("결제 대기 시간 만료 스케줄러 시작");

        // Timer를 사용하여 작업 실행 시간을 측정
        reservationSchedulerTimer.record(() -> {
            try {
                reservationService.expireOldPendingPayments();
            } catch (Exception e) {
                // 예외 발생 시 로그 및 추가 메트릭 처리 가능
                log.error("스케줄러 실행 중 오류 발생", e);
                // (선택) 에러 카운터 메트릭을 추가할 수도 있습니다.
                // meterRegistry.counter("scheduler.reservation.expiration.errors").increment();
            }
        });

        log.info("결제 대기 시간 만료 스케줄러 종료");
    }
}