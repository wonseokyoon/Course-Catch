package com.Catch_Course.domain.reservation.controller;

import com.Catch_Course.domain.course.entity.Course;
import com.Catch_Course.domain.course.service.CourseService;
import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.member.service.MemberService;
import com.Catch_Course.domain.reservation.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class ReservationControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private MemberService memberService;

    private String token;
    private Member loginedMember;

    @BeforeEach
    @DisplayName("user1로 로그인 셋업")
    void setUp() {
        loginedMember = memberService.findByUsername("user1").get();
        token = memberService.getAuthToken(loginedMember);
    }

    @Test
    @DisplayName("수강 신청")
    void reserve() throws Exception {
        Long courseId = 1L;
        ResultActions resultActions = mvc.perform(
                        post("/api/reserve?courseId=%d".formatted(courseId))
                                .header("Authorization", "Bearer " + token)
                ).andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("신청이 완료되었습니다."));

        Course course = courseService.getItem(courseId).get();
        resultActions
                .andExpect(jsonPath("$.data.courseId").value(courseId))
                .andExpect(jsonPath("$.data.courseTitle").value(course.getTitle()))
                .andExpect(jsonPath("$.data.studentId").value(loginedMember.getId()))
                .andExpect(jsonPath("$.data.studentName").value(loginedMember.getNickname()))
        ;
    }
}