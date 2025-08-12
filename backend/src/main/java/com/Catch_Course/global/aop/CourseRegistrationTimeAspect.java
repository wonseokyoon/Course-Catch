package com.Catch_Course.global.aop;

import com.Catch_Course.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class CourseRegistrationTimeAspect {

    @Value("${app.registration-time.start}")
    private String startTimeString; // "19:00" 문자열을 주입받음

    @Value("${app.registration-time.duration-in-minutes}")
    private int durationInMinutes; // 60 숫자를 주입받음

    @Before("@annotation(com.Catch_Course.global.aop.CheckTime)")
    public void checkTime() {
        LocalDateTime now = LocalDateTime.now();

        LocalTime configuredStartTime = LocalTime.parse(startTimeString);
        LocalDateTime startTime = now.with(configuredStartTime);
        LocalDateTime endTime = startTime.plusMinutes(durationInMinutes);

        log.info("수강 신청 시도: 현재 시간={}, 허용 구간: {} ~ {}", now, startTime, endTime);

        if (now.isBefore(startTime) || !now.isBefore(endTime)) {
            throw new ServiceException("403-1", "수강 신청 가능한 시간이 아닙니다. (매일 09:00 ~ 09:59)");
        }
    }
}
