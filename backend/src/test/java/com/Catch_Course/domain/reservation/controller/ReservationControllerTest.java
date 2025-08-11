//package com.Catch_Course.domain.reservation.controller;
//
//import com.Catch_Course.domain.course.entity.Course;
//import com.Catch_Course.domain.course.service.CourseService;
//import com.Catch_Course.domain.member.entity.Member;
//import com.Catch_Course.domain.member.service.MemberService;
//import com.Catch_Course.domain.reservation.service.ReservationService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.ResultActions;
//import org.springframework.transaction.annotation.Transactional;
//import org.testcontainers.containers.GenericContainer;
//import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//
//import static org.hamcrest.Matchers.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@SpringBootTest
//@ActiveProfiles("test")
//@AutoConfigureMockMvc
//@Transactional
//@Testcontainers
//class ReservationControllerTest {
//    @Autowired
//    private MockMvc mvc;
//
//    @Autowired
//    private ReservationService reservationService;
//
//    @Autowired
//    private CourseService courseService;
//
//    @Autowired
//    private MemberService memberService;
//
//    private String token;
//    private Member loginedMember;
//    private Member member2;
//    private String token2;
//
//    // Redis 컨테이너 생성 및 포트 설정
//    @Container
//    private static final GenericContainer<?> REDIS_CONTAINER =
//            new GenericContainer<>("redis:6-alpine")
//                    .withExposedPorts(6379)
//                    .waitingFor(new WaitAllStrategy());
//
//    // RedisTemplate이 컨테이너의 동적 포트를 사용하도록 설정
//    @DynamicPropertySource
//    static void setRedisProperties(DynamicPropertyRegistry registry) {
//        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
//        registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379));
//    }
//
//    @BeforeEach
//    @DisplayName("user1로 로그인 셋업")
//    void setUp() {
//        loginedMember = memberService.findByUsername("user1").get();
//        token = memberService.getAuthToken(loginedMember);
//    }
//
//    @DisplayName("user2로 로그인")
//    void loginUser2() throws Exception {
//        member2 = memberService.findByUsername("user2").get();
//        token2 = memberService.getAuthToken(member2);
//    }
//
//    @Test
//    @DisplayName("수강 신청")
//    void reserve() throws Exception {
//        Long courseId = 1L;
//        ResultActions resultActions = mvc.perform(
//                post("/api/reserve?courseId=%d".formatted(courseId))
//                        .header("Authorization", "Bearer " + token)
//        ).andDo(print());
//
//        resultActions
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.code").value("200-1"))
//                .andExpect(jsonPath("$.msg").value("신청이 완료되었습니다."));
//
//        Course course = courseService.getItem(courseId).get();
//        resultActions
//                .andExpect(jsonPath("$.data.courseId").value(courseId))
//                .andExpect(jsonPath("$.data.courseTitle").value(course.getTitle()))
//                .andExpect(jsonPath("$.data.studentId").value(loginedMember.getId()))
//                .andExpect(jsonPath("$.data.studentName").value(loginedMember.getNickname()))
//        ;
//    }
//
//    @Test
//    @DisplayName("수강 신청 실패 - 이미 신청한 강의")
//    void reserve2() throws Exception {
//        Long courseId = 1L;
//        reservationService.reserve(loginedMember, courseId);    // 수강 신청
//
//        ResultActions resultActions = mvc.perform(
//                post("/api/reserve?courseId=%d".formatted(courseId))
//                        .header("Authorization", "Bearer " + token)
//        ).andDo(print());
//
//        resultActions
//                .andExpect(status().isConflict())
//                .andExpect(jsonPath("$.code").value("409-1"))
//                .andExpect(jsonPath("$.msg").value("이미 신청한 강의입니다."));
//    }
//
//    @Test
//    @DisplayName("수강 신청 실패 - 없는 강의")
//    void reserve3() throws Exception {
//        Long courseId = 999L;
//        ResultActions resultActions = mvc.perform(
//                post("/api/reserve?courseId=%d".formatted(courseId))
//                        .header("Authorization", "Bearer " + token)
//        ).andDo(print());
//
//        resultActions
//                .andExpect(status().isNotFound())
//                .andExpect(jsonPath("$.code").value("404-1"))
//                .andExpect(jsonPath("$.msg").value("존재하지 않는 강의입니다."));
//    }
//
//    @Test
//    @DisplayName("수강 신청 실패 - 자리가 없음")
//    void reserve4() throws Exception {
//        Long courseId = 3L;
//        reservationService.reserve(loginedMember, courseId);    // 수강 신청
//
//        loginUser2();   // 계정 바꿔서 로그인
//
//        ResultActions resultActions = mvc.perform(
//                post("/api/reserve?courseId=%d".formatted(courseId))
//                        .header("Authorization", "Bearer " + token2)
//        ).andDo(print());
//
//        resultActions
//                .andExpect(status().isNotAcceptable())
//                .andExpect(jsonPath("$.code").value("406-1"))
//                .andExpect(jsonPath("$.msg").value("남은 좌석이 없습니다."));
//    }
//
//    @Test
//    @DisplayName("수강 취소")
//    void cancelReservation() throws Exception {
//        Long courseId = 1L;
//        reservationService.reserve(loginedMember, courseId);    // 수강 신청
//
//        ResultActions resultActions = mvc.perform(
//                delete("/api/reserve?courseId=%d".formatted(courseId))
//                        .header("Authorization", "Bearer " + token)
//        ).andDo(print());
//
//        resultActions
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.code").value("200-1"))
//                .andExpect(jsonPath("$.msg").value("수강 취소되었습니다."));
//    }
//
//    @Test
//    @DisplayName("수강 취소 실패 - 이미 취소")
//    void cancelReservation2() throws Exception {
//        Long courseId = 1L;
//        cancelReservation();    // 수강 취소
//
//        ResultActions resultActions = mvc.perform(
//                delete("/api/reserve?courseId=%d".formatted(courseId))
//                        .header("Authorization", "Bearer " + token)
//        ).andDo(print());
//
//        resultActions
//                .andExpect(status().isConflict())
//                .andExpect(jsonPath("$.code").value("409-2"))
//                .andExpect(jsonPath("$.msg").value("이미 취소된 수강 신청입니다."));
//    }
//
//    @Test
//    @DisplayName("수강 취소 실패 - 이력이 없음")
//    void cancelReservation3() throws Exception {
//        Long courseId = 3L;
//
//        ResultActions resultActions = mvc.perform(
//                delete("/api/reserve?courseId=%d".formatted(courseId))
//                        .header("Authorization", "Bearer " + token)
//        ).andDo(print());
//
//        resultActions
//                .andExpect(status().isNotFound())
//                .andExpect(jsonPath("$.code").value("404-3"))
//                .andExpect(jsonPath("$.msg").value("수강 신청 이력이 없습니다."));
//    }
//
//    @Test
//    @DisplayName("수강 취소 실패 - 존재하지 않는 강의")
//    void cancelReservation4() throws Exception {
//        Long courseId = 999L;
//
//        ResultActions resultActions = mvc.perform(
//                delete("/api/reserve?courseId=%d".formatted(courseId))
//                        .header("Authorization", "Bearer " + token)
//        ).andDo(print());
//
//        resultActions
//                .andExpect(status().isNotFound())
//                .andExpect(jsonPath("$.code").value("404-1"))
//                .andExpect(jsonPath("$.msg").value("존재하지 않는 강의입니다."));
//    }
//
//    @Test
//    @DisplayName("신청 목록 조회")
//    void getReservation() throws Exception {
//        int page = 1;
//        int pageSize = 5;
//
//        Long courseId = 1L;
//        mvc.perform(       // 수강 신청
//                post("/api/reserve?courseId=%d".formatted(courseId))
//                        .header("Authorization", "Bearer " + token)
//        );
//
//        ResultActions resultActions = mvc.perform(
//                get("/api/reserve/me?page=%d&pageSize=%d".formatted(page, pageSize))
//                        .header("Authorization", "Bearer " + token)
//        );
//
//        resultActions
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.code").value("200-1"))
//                .andExpect(jsonPath("$.msg").value("신청 목록 조회가 완료되었습니다."))
//                .andExpect(jsonPath("$.data.items.length()", lessThanOrEqualTo(pageSize)))
//                .andExpect(jsonPath("$.data.currentPage").value(page))
//                .andExpect(jsonPath("$.data[*].studentName").value(everyItem(equalTo(loginedMember.getNickname()))))
//
//        ;
//    }
//
//    @Test
//    @DisplayName("신청 목록 조회 실패 - 신청 내역이 없음")
//    void getReservation2() throws Exception {
//        ResultActions resultActions = mvc.perform(
//                get("/api/reserve/me")
//                        .header("Authorization", "Bearer " + token)
//        );
//
//        resultActions
//                .andExpect(status().isNotFound())
//                .andExpect(jsonPath("$.code").value("404-3"))
//                .andExpect(jsonPath("$.msg").value("수강신청 이력이 없습니다."))
//        ;
//    }
//}