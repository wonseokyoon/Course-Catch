//package com.Catch_Course.domain.reservation.controller;
//
//import com.Catch_Course.domain.member.entity.Member;
//import com.Catch_Course.domain.member.service.MemberService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.ResultActions;
//
//import java.time.ZoneId;
//import java.time.ZonedDateTime;
//import java.util.function.Supplier;
//
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@SpringBootTest
//@ActiveProfiles("test")
//@AutoConfigureMockMvc
//public class ReservationTimeOutTest {
//    @Autowired
//    private MockMvc mvc;
//
//    @Autowired
//    private MemberService memberService;
//
//    private String token;
//    private Member loginedMember;
//
//    @MockitoBean
//    private Supplier<ZonedDateTime> clockSupplier;
//
//    @BeforeEach
//    @DisplayName("user1로 로그인 셋업")
//    void setUp() {
//        loginedMember = memberService.findByUsername("user1").get();
//        token = memberService.getAuthToken(loginedMember);
//    }
//
//    @Test
//    @DisplayName("수강신청 실패 - 지정 시간 이외에 신청")
//    void reserve5() throws Exception {
//        Long courseId = 1L;
//        // 고정 시간
//        ZonedDateTime fixedTime = ZonedDateTime.of(2025, 8, 12, 6, 0, 0, 0, ZoneId.of("Asia/Seoul"));
//        when(clockSupplier.get()).thenReturn(fixedTime);
//
//        ResultActions resultActions = mvc.perform(
//                post("/api/reserve?courseId=%d".formatted(courseId))
//                        .header("Authorization", "Bearer " + token)
//        ).andDo(print());
//
//        resultActions
//                .andExpect(status().isForbidden())
//                .andExpect(jsonPath("$.code").value("403-1"))
//                .andExpect(jsonPath("$.msg").value("수강 신청 가능한 시간이 아닙니다. (매일 09:00 ~ 09:59)"));
//    }
//}
