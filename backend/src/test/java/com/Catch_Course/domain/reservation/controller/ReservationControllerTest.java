package com.Catch_Course.domain.reservation.controller;

import com.Catch_Course.domain.course.entity.Course;
import com.Catch_Course.domain.course.service.CourseService;
import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.member.service.MemberService;
import com.Catch_Course.domain.notification.dto.NotificationDto;
import com.Catch_Course.domain.notification.service.NotificationService;
import com.Catch_Course.domain.reservation.entity.Reservation;
import com.Catch_Course.domain.reservation.entity.ReservationStatus;
import com.Catch_Course.domain.reservation.repository.ReservationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnableAsync
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
class ReservationControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private CourseService courseService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ReservationTestHelper reservationTestHelper;

    @MockitoBean
    private Supplier<ZonedDateTime> clockSupplier;

    private String token;
    private Member loginedMember;
    private Member loginedMember2;
    private String token2;

    @Container
    private static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    // Redis 컨테이너 생성 및 포트 설정
    @Container
    private static final GenericContainer<?> REDIS_CONTAINER =
            new GenericContainer<>("redis:6-alpine")
                    .withExposedPorts(6379)
                    .waitingFor(Wait.forListeningPort());

    // RedisTemplate이 컨테이너의 동적 포트를 사용하도록 설정
    @DynamicPropertySource
    static void setRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379));
        registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
    }

    @BeforeEach
    @DisplayName("user1로 로그인 셋업")
    void setUp() {
        loginedMember = memberService.findByUsername("user1").get();
        token = memberService.getAuthToken(loginedMember);
        when(clockSupplier.get()).thenAnswer(invocation -> ZonedDateTime.now(ZoneId.of("Asia/Seoul")));
    }

    @AfterEach
    void tearDown() {
        reservationRepository.deleteAllInBatch();
    }

    @DisplayName("user35로 로그인")
    void loginUser2() throws Exception {
        loginedMember2 = memberService.findByUsername("user35").get();
        token2 = memberService.getAuthToken(loginedMember2);
    }

    @Test
    @DisplayName("수강 신청 - 대기열 등록")
    void addQueue() throws Exception {
        Long courseId = 1L;

        ResultActions resultActions = mvc.perform(
                post("/api/reserve?courseId=%d".formatted(courseId))
                        .header("Authorization", "Bearer " + token)
        ).andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("신청이 접수되었습니다. 잠시 기다려주세요."));
    }

    @Test
    @DisplayName("수강 신청 - 성공 - SSE 이벤트 수신")
    void reserve() throws Exception {
        Long courseId = 1L;
        Course course = courseService.findById(courseId);
        reservationTestHelper.reserveSetUp(loginedMember, course);

        Course awaitCourse = courseService.findById(courseId);
        // DB 조회
        Reservation reservation = reservationRepository.findByStudentAndCourse(loginedMember, awaitCourse).get();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);

        // RedisStream 확인
        List<NotificationDto> events = notificationService.getNotifications(loginedMember.getId());
        NotificationDto event = events.get(events.size() - 1);  // 최신 이벤트
        assertThat(event.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(event.getMessage()).isEqualTo("수강 신청이 성공하였습니다.");
        assertThat(event.getCourseTitle()).isEqualTo(awaitCourse.getTitle());
    }

    @Test
    @DisplayName("수강 신청 실패 - 이미 신청한 강의")
    void reserve2() throws Exception {
        Long courseId = 1L;
        Course course = courseService.findById(courseId);
        reservationTestHelper.reserveSetUp(loginedMember, course);

        ResultActions resultActions = mvc.perform(
                post("/api/reserve?courseId=%d".formatted(courseId))
                        .header("Authorization", "Bearer " + token)
        ).andDo(print());

        resultActions
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("409-1"))
                .andExpect(jsonPath("$.msg").value("이미 신청한 강의입니다."));
    }

    @Test
    @DisplayName("수강 신청 실패 - 없는 강의")
    void reserve3() throws Exception {
        Long courseId = 999L;

        ResultActions resultActions = mvc.perform(
                post("/api/reserve?courseId=%d".formatted(courseId))
                        .header("Authorization", "Bearer " + token)
        ).andDo(print());

        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("404-1"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 강의입니다."));
    }

    @Test
    @DisplayName("수강 신청 실패 - 자리가 없음")
    void reserve4() throws Exception {
        Long courseId = 52L;
        reservationTestHelper.currentRegistrationSetUp(courseId);
        Course course = courseService.findById(courseId);
        loginUser2();   // 계정 바꿔서 로그인
        reservationTestHelper.reserveSetUp(loginedMember2,course);
//        mvc.perform(post("/api/reserve?courseId=%d".formatted(courseId))
//                        .header("Authorization", "Bearer " + token2))
//                .andExpect(status().isOk());

        Course awaitCourse = courseService.findById(courseId);
        // DB 조회
        Reservation reservation = reservationRepository.findByStudentAndCourse(loginedMember2, awaitCourse).get();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.FAILED);

        // RedisStream 확인
        List<NotificationDto> events = notificationService.getNotifications(loginedMember2.getId());
        NotificationDto event = events.get(events.size() - 1);  // 최신 이벤트
        assertThat(event.getStatus()).isEqualTo(ReservationStatus.FAILED);
        assertThat(event.getMessage()).isEqualTo("수강 신청 실패: 정원이 마감되었습니다.");
        assertThat(event.getCourseTitle()).isEqualTo(awaitCourse.getTitle());
    }

    @Test
    @DisplayName("수강 취소 - Pending 상태")
    void cancelReservation() throws Exception {
        Long courseId = 1L;
        Course course = courseService.findById(courseId);
        reservationTestHelper.reserveSetUp(loginedMember, course);

        ResultActions resultActions = mvc.perform(
                delete("/api/reserve?courseId=%d".formatted(courseId))
                        .header("Authorization", "Bearer " + token)
        ).andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("수강 취소 요청이 접수되었습니다."));
    }

    @Test
    @DisplayName("수강 취소 실패 - 이미 취소")
    void cancelReservation2() throws Exception {
        Long courseId = 1L;
        cancelReservation();    // 수강 취소
        Thread.sleep(1000);

        ResultActions resultActions = mvc.perform(
                delete("/api/reserve?courseId=%d".formatted(courseId))
                        .header("Authorization", "Bearer " + token)
        ).andDo(print());

        resultActions
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("409-5"))
                .andExpect(jsonPath("$.msg").value("이미 취소된 신청입니다."));
    }

    @Test
    @DisplayName("수강 취소 실패 - Reservation: Failed 상태")
    void cancelReservation3() throws Exception {
        Long courseId = 1L;
        Course course = courseService.findById(courseId);
        reservationTestHelper.reserveSetUpFailed(loginedMember, course);
        Thread.sleep(1000); // 잠시 대기

        ResultActions resultActions = mvc.perform(
                delete("/api/reserve?courseId=%d".formatted(courseId))
                        .header("Authorization", "Bearer " + token)
        ).andDo(print());

        resultActions
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("409-6"))
                .andExpect(jsonPath("$.msg").value("취소할 수 없는 신청입니다."));
    }

    @Test
    @DisplayName("수강 취소 실패 - Reservation: Waiting 상태")
    void cancelReservation4() throws Exception {
        Long courseId = 1L;
        Course course = courseService.findById(courseId);
        reservationTestHelper.reserveSetUpWaiting(loginedMember, course);

        ResultActions resultActions = mvc.perform(
                delete("/api/reserve?courseId=%d".formatted(courseId))
                        .header("Authorization", "Bearer " + token)
        ).andDo(print());

        resultActions
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("409-6"))
                .andExpect(jsonPath("$.msg").value("취소할 수 없는 신청입니다."));
    }

    @Test
    @DisplayName("수강 취소 실패 - 이력이 없음")
    void cancelReservation5() throws Exception {
        Long courseId = 3L;

        ResultActions resultActions = mvc.perform(
                delete("/api/reserve?courseId=%d".formatted(courseId))
                        .header("Authorization", "Bearer " + token)
        ).andDo(print());

        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("404-3"))
                .andExpect(jsonPath("$.msg").value("수강 신청 이력이 없습니다."));
    }

    @Test
    @DisplayName("수강 취소 실패 - 존재하지 않는 강의")
    void cancelReservation6() throws Exception {
        Long courseId = 999L;

        ResultActions resultActions = mvc.perform(
                delete("/api/reserve?courseId=%d".formatted(courseId))
                        .header("Authorization", "Bearer " + token)
        ).andDo(print());

        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("404-1"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 강의입니다."));
    }

    @Test
    @DisplayName("수강 목록 조회(결제 대기)")
    void getReservation() throws Exception {
        int page = 1;
        int pageSize = 5;

        Long courseId = 1L;
        Course course = courseService.findById(courseId);
        reservationTestHelper.reserveSetUp(loginedMember, course);

        ResultActions resultActions = mvc.perform(
                get("/api/reserve/me/pending?page=%d&pageSize=%d".formatted(page, pageSize))
                        .header("Authorization", "Bearer " + token)
        );

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("신청 목록 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.data.items.length()", lessThanOrEqualTo(pageSize)))
                .andExpect(jsonPath("$.data.currentPage").value(page))
                .andExpect(jsonPath("$.data[*].studentName").value(everyItem(equalTo(loginedMember.getNickname()))))
        ;
    }

    @Test
    @DisplayName("신청 목록 조회 실패 - 신청 내역이 없음")
    void getReservation2() throws Exception {
        ResultActions resultActions = mvc.perform(
                get("/api/reserve/me")
                        .header("Authorization", "Bearer " + token)
        );

        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("404-3"))
                .andExpect(jsonPath("$.msg").value("수강신청 이력이 없습니다."))
        ;
    }

    @Test
    @DisplayName("수강신청 실패 - 지정 시간 이외에 신청")
    void reserve5() throws Exception {
        Long courseId = 1L;
        // 고정 시간
        ZonedDateTime fixedTime = ZonedDateTime.of(2025, 8, 12, 6, 0, 0, 0, ZoneId.of("Asia/Seoul"));
        when(clockSupplier.get()).thenReturn(fixedTime);

        ResultActions resultActions = mvc.perform(
                post("/api/reserve?courseId=%d".formatted(courseId))
                        .header("Authorization", "Bearer " + token)
        ).andDo(print());

        resultActions
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("403-1"))
                .andExpect(jsonPath("$.msg").value("수강 신청 가능한 시간이 아닙니다. (매일 09:00 ~ 09:59)"));
    }

}