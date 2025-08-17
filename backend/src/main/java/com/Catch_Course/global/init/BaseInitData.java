package com.Catch_Course.global.init;

import com.Catch_Course.domain.course.service.CourseService;
import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@RequiredArgsConstructor
public class BaseInitData {

    private final CourseService courseService;
    private final MemberService memberService;

    @Autowired
    @Lazy
    private BaseInitData self;

    @Bean
    public ApplicationRunner applicationRunner() {
        return args -> {
            self.memberInit();
            self.courseInit();
        };
    }

    @Transactional
    public void memberInit() {

        if (memberService.count() > 0) {
            return;
        }

        for (int i = 1; i <= 100; i++) {
            memberService.join("user%d".formatted(i), "user%d1234".formatted(i), "유저%d".formatted(i), "user%d@example.com".formatted(i), "");
        }

        // 회원 샘플데이터 생성
        memberService.join("system", "system1234", "시스템", "system@example.com", "https://placehold.co/640x640?text=system");
        memberService.join("admin", "admin1234", "관리자", "admin@example.com", "https://placehold.co/640x640?text=admin");
    }

    @Transactional
    public void courseInit() {

        if (courseService.count() > 0) {
            return;
        }

        Member user2 = memberService.findByUsername("user2").get();

        courseService.write(user2, "수학 강의", "일반 수학 기초", 50,10000);
        courseService.write(user2, "국어 강의", "비문학 기초", 100,20000);
        courseService.write(user2, "인기 폭발 강의", "늦으면 없다", 1,5000);

        for (int i = 2; i < 50; i++) {
            int userId = (i % 5) + 2;
            Member tempUser = memberService.findByUsername("user%d".formatted(userId)).get();
            courseService.write(tempUser, "강의 제목 %d".formatted(i), "강의 내용 %d".formatted(i), 50,1000 * i);
        }
        courseService.write(user2, "인기 폭발123 강의", "늦으면 없213다", 1,500000);
    }
}
