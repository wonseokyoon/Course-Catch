package com.Catch_Course.domain.course.controller;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.member.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class CourseControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberService memberService;

    private String accessToken;
    private Member loginedMember;

    @BeforeEach
    @DisplayName("user1로 로그인 셋업")
    void setUp() {
        loginedMember = memberService.findByUsername("user1").get();
        accessToken = memberService.getAccessToken(loginedMember);
    }

    @Test
    @DisplayName("강의 목록 조회")
    void getItems() {
    }

    @Test
    @DisplayName("강의 상세 조회")
    void getItem() {
    }

    @Test
    @DisplayName("강의 삭제 - 작성자만 삭제 가능")
    void delete() {
    }

    @Test
    @DisplayName("강의 수정 - 작성자만 수정 가능")
    void modify() {
    }

    @Test
    @DisplayName("강의 생성")
    void write() {
    }

    String adminLogin() {
        loginedMember = memberService.findByUsername("admin").get();
        return memberService.getAccessToken(loginedMember);
    }

    @Test
    @DisplayName("통계 - 관리자 기능 - 관리자 접근")
    @WithUserDetails("admin")   // CustomUserDetailService 의 loadUserByUsername 메서드를 통해 유저 정보 가져옴
    void getStatistics() throws Exception {
//        accessToken = adminLogin();
        ResultActions resultActions = mvc.perform(
                        get("/api/courses/statistics")
                                // @WithUserDetails("admin") 으로 로그인 한 것처럼 되기때문에, 헤더를 굳이 입력할 필요 x
//                                .header("Authorization", "Bearer " + accessToken)
                )
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("통계 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.data.courseCount").value(20))
                .andExpect(jsonPath("$.data.publishedCount").value(10));
    }

    @Test
    @DisplayName("통계 - 관리자 기능 - user1 접근")
    @WithUserDetails("user1")
    void statisticsUser() throws Exception {
        ResultActions resultActions = mvc.perform(
                        get("/api/courses/statistics")
//                                .header("Authorization", "Bearer " + accessToken)
                )
                .andDo(print());

        resultActions
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("403-1"))
                .andExpect(jsonPath("$.msg").value("접근 권한이 없습니다."));

    }

}
