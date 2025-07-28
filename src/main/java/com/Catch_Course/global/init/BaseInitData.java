package com.Catch_Course.global.init;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.member.service.MemberService;
import com.Catch_Course.domain.course.entity.Course;
import com.Catch_Course.domain.course.service.CourseService;
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
            self.postInit();
        };
    }

    @Transactional
    public void memberInit() {

        if(memberService.count() > 0) {
            return;
        }

        // 회원 샘플데이터 생성
        memberService.join("system", "system1234", "시스템");
        memberService.join("admin", "admin1234", "관리자");
        memberService.join("user1", "user11234", "유저1");
        memberService.join("user2", "user21234", "유저2");
        memberService.join("user3", "user31234", "유저3");

    }

    @Transactional
    public void postInit() {

        if(courseService.count() > 0) {
            return;
        }

        Member user1 = memberService.findByUsername("user1").get();
        Member user2 = memberService.findByUsername("user2").get();

        Course course1 = courseService.write(user1, "축구 하실분 모집합니다.", "저녁 6시까지 모여주세요.");
        Course course2 = courseService.write(user1, "농구하실분?", "3명 모집");

        courseService.write(user2, "title3", "content3");

    }



}
