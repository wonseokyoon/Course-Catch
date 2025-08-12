package com.Catch_Course.global.aop;

import com.Catch_Course.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.function.Supplier;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class CourseRegistrationTimeAspect {

    private final Supplier<ZonedDateTime> clockSupplier;

    @Value("${app.registration-time.start}")
    private String startTimeString; // 지정 시간

    @Value("${app.registration-time.duration-in-minutes}")
    private int durationInMinutes; // 유효기간 주입

    @Before("@annotation(com.Catch_Course.global.aop.CheckTime)")
    public void checkTime() {
        ZonedDateTime now = clockSupplier.get();
        LocalTime configuredStartTime = LocalTime.parse(startTimeString);
        ZonedDateTime startTime = ZonedDateTime.of(now.toLocalDate(), configuredStartTime, now.getZone());
        ZonedDateTime endTime = startTime.plusMinutes(durationInMinutes);

        log.info("수강 신청 시도: 현재 시간={}, 허용 구간: {} ~ {}", now, startTime, endTime);

        if (now.isBefore(startTime) || !now.isBefore(endTime)) {
            throw new ServiceException("403-1", "수강 신청 가능한 시간이 아닙니다. (매일 09:00 ~ 09:59)");
        }
    }
}
