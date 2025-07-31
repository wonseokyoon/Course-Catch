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

        // 회원 샘플데이터 생성
        memberService.join("system", "system1234", "시스템");
        memberService.join("admin", "admin1234", "관리자");
        memberService.join("user1", "user11234", "유저1");
        memberService.join("user2", "user21234", "유저2");
        memberService.join("user3", "user31234", "유저3");

        for (int i = 4; i < 50; i++) {
            memberService.join("user%d".formatted(i), "user%d1234".formatted(i), "유저%d".formatted(i));
        }

    }

    @Transactional
    public void courseInit() {

        if (courseService.count() > 0) {
            return;
        }

        Member user1 = memberService.findByUsername("user1").get();

        courseService.write(user1, "수학 강의", "일반 수학 기초", 50);
        courseService.write(user1, "국어 강의", "비문학 기초", 100);

        for (int i = 2; i < 50; i++) {
            Member tempUser = memberService.findByUsername("user%d".formatted(i)).get();
            courseService.write(tempUser, "강의 제목 %d".formatted(i), "강의 내용 %d".formatted(i), 50);
        }
    }


}
