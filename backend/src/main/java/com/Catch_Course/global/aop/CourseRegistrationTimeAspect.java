package com.Catch_Course.global.aop;

import com.Catch_Course.domain.course.repository.CourseRepository;
import com.Catch_Course.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class CourseRegistrationTimeAspect {

    private final CourseRepository courseRepository;

    @Before("@annotation(com.Catch_Course.global.aop.CheckTime)")
    public void checkTime() {
        LocalDateTime startTime = LocalDate.now().atTime(LocalTime.of(19, 10));
        LocalDateTime endTime = startTime.plusMinutes(60);
        LocalDateTime now = LocalDateTime.now();

        log.info("수강 신청 시도: 현재 시간={}, 허용 구간: {} ~ {}", now, startTime, endTime);

        if (now.isBefore(startTime) || !now.isBefore(endTime)) {
            throw new ServiceException("403-1", "수강 신청 가능한 시간이 아닙니다. (매일 09:00 ~ 09:59)");
        }
    }
}
