package com.Catch_Course.domain.payments.controller;

import com.Catch_Course.domain.course.entity.Course;
import com.Catch_Course.domain.course.repository.CourseRepository;
import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.member.service.MemberService;
import com.Catch_Course.domain.payments.entity.Payment;
import com.Catch_Course.domain.payments.entity.PaymentStatus;
import com.Catch_Course.domain.payments.repository.PaymentRepository;
import com.Catch_Course.domain.reservation.entity.Reservation;
import com.Catch_Course.domain.reservation.entity.ReservationStatus;
import com.Catch_Course.domain.reservation.repository.ReservationRepository;
import com.Catch_Course.global.exception.ServiceException;
import com.Catch_Course.global.kafka.producer.ReservationCancelProducer;
import com.Catch_Course.global.payment.TossPaymentsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnableAsync
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
class PaymentControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberService memberService;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoBean
    private ReservationCancelProducer reservationCancelProducer;

    @MockitoBean
    private TossPaymentsService tossPaymentsService;

    @MockitoBean
    private Supplier<ZonedDateTime> clockSupplier;

    private String token;
    private Member loginedMember;
    private Reservation reservation;

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

        Course course = courseRepository.findById(1L).get();

        // 결제 대기중인 예약 세팅
        reservation = Reservation.builder()
                .student(loginedMember)
                .course(course)
                .status(ReservationStatus.PENDING)
                .price(course.getPrice())
                .build();

        reservationRepository.save(reservation);
    }

    private ResultActions confirmRequest(String paymentKey, String orderId, long amount) throws Exception {
        Map<String, String> requestBody = Map.of("paymentKey", paymentKey, "orderId", orderId,"amount", String.valueOf(amount));

        // Map -> Json 변환
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(requestBody);

        return mvc
                .perform(
                        post("/api/payment/confirm")
                                .content(json)
                                .contentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
                                .header("Authorization", "Bearer " + token)
                ).andDo(print());
    }

    @Test
    @DisplayName("결제 성공 시나리오")
    void paymentSuccess() throws Exception {

        ResultActions requestActions = mvc.perform(
                post("/api/payment/request")
                        .param("reservationId", String.valueOf(reservation.getId()))
                        .header("Authorization", "Bearer " + token)
        ).andDo(print());

        ObjectMapper mapper = new ObjectMapper();
        String jsonContent = requestActions.andReturn().getResponse().getContentAsString();
        JsonNode json = mapper.readTree(jsonContent);   // 파싱

        String orderId = json.get("data").get("orderId").asText();
        long amount = json.get("data").get("amount").asLong();

        doNothing().when(tossPaymentsService).confirm(any(), any(), any());
        ResultActions confirmActions = confirmRequest("fake-payment-key", orderId,amount);

        confirmActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("신청 목록 조회가 완료되었습니다."));

        Payment confirmedPayment = paymentRepository.findByMerchantUid(orderId).get();
        Reservation completedReservation = reservationRepository.findById(reservation.getId()).get();

        assertThat(confirmedPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(completedReservation.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
    }

    @Test
    @DisplayName("결제 취소 성공 시나리오")
    void cancelSuccess() throws Exception {
        paymentSuccess();   // 결제 성공 셋업

        ResultActions requestActions = mvc.perform(
                delete("/api/payment/{reservationId}", reservation.getId())
                        .header("Authorization", "Bearer " + token)
        ).andDo(print());

        requestActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("결제 취소요청이 접수되었습니다."));

        Payment canceledPayment = paymentRepository.findByReservation(reservation).get();
        assertThat(canceledPayment.getStatus()).isEqualTo(PaymentStatus.CANCEL_REQUESTED);
    }

    @Test
    @DisplayName("E2E 테스트: 결제 취소 요청 시 최종적으로 결제와 예약 상태가 모두 CANCELLED로 변경된다")
    void paymentAndReservation_Should_Be_Cancelled_After_CancelRequest() throws Exception {
        reservation.setStatus(ReservationStatus.COMPLETED);
        Course course = reservation.getCourse();
        course.setCurrentRegistration(course.getCapacity());
        courseRepository.save(course);
        reservationRepository.save(reservation);

        Payment payment = Payment.builder()
                .reservation(reservation)
                .member(loginedMember)
                .status(PaymentStatus.PAID)
                .merchantUid("e2e-test-merchant-uid-" + System.currentTimeMillis())
                .paymentKey("e2e-test-payment-key")
                .amount(reservation.getPrice())
                .build();
        paymentRepository.save(payment);

        doNothing().when(tossPaymentsService).cancel(any(), any());

        mvc.perform(
                        delete("/api/payment/{reservationId}", reservation.getId())
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCEL_REQUESTED"));


        Awaitility.await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Payment finalizedPayment = paymentRepository.findById(payment.getId()).get();
            Reservation finalizedReservation = reservationRepository.findById(reservation.getId()).get();

            assertThat(finalizedPayment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
            assertThat(finalizedReservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        });
    }

    @Test
    @DisplayName("실패 시나리오: 외부 서비스 환불 실패, Payment 상태는 CANCEL_REQUESTED로 유지된다")
    void paymentCancel_Fails_When_PG_Fails() throws Exception {
        // GIVEN: '결제 완료' 상태의 예약과 결제 데이터 셋업
        reservation.setStatus(ReservationStatus.COMPLETED);
        reservationRepository.save(reservation);

        Payment payment = Payment.builder()
                .reservation(reservation)
                .member(loginedMember)
                .status(PaymentStatus.PAID)
                .merchantUid("fail-test-merchant-uid-" + System.currentTimeMillis())
                .paymentKey("fail-test-payment-key")
                .amount(reservation.getPrice())
                .build();
        paymentRepository.save(payment);

        // GIVEN: ⭐️ TossPaymentsService의 cancel 메서드가 항상 예외를 던지도록 Mocking
        doThrow(new ServiceException("400-6", "PG사 연동 오류"))
                .when(tossPaymentsService).cancel(any(), any());

        // WHEN: 사용자가 결제 취소 API를 호출
        mvc.perform(
                delete("/api/payment/{reservationId}", reservation.getId())
                        .header("Authorization", "Bearer " + token)
        ).andExpect(status().isOk());


        // 최종적으로 상태가 변경되지 않았음을 검증
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Payment failedPayment = paymentRepository.findById(payment.getId()).get();
                    Reservation notCancelledReservation = reservationRepository.findById(reservation.getId()).get();

                    // 최종적으로 상태가 변경되지 않았음을 검증
                    assertThat(failedPayment.getStatus()).isEqualTo(PaymentStatus.CANCEL_REQUESTED);
                    assertThat(notCancelledReservation.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
                });
    }

    @Test
    @DisplayName("실패 시나리오: 외부 서비스 환불은 성공했으나 내부 DB 작업 실패 시, 상태는 CANCEL_REQUESTED로 유지된다")
    void paymentCancel_Rollbacks_When_InternalDB_Fails() throws Exception {
        // GIVEN: '결제 완료' 상태의 예약과 결제 데이터 셋업
        reservation.setStatus(ReservationStatus.COMPLETED);
        reservationRepository.save(reservation);
        Payment payment = Payment.builder()
                .reservation(reservation)
                .member(loginedMember)
                .status(PaymentStatus.PAID)
                .merchantUid("internal-fail-uid-" + System.currentTimeMillis())
                .paymentKey("internal-fail-key")
                .amount(reservation.getPrice())
                .build();
        paymentRepository.save(payment);

        doNothing().when(tossPaymentsService).cancel(any(), any());
        doThrow(new RuntimeException("내부 DB 저장 실패!"))
                .when(reservationCancelProducer).send(any());

        mvc.perform(
                delete("/api/payment/{reservationId}", reservation.getId())
                        .header("Authorization", "Bearer " + token)
        ).andExpect(status().isOk());


        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Payment rolledBackPayment = paymentRepository.findById(payment.getId()).get();

                    // 최초 요청 상태인 CANCEL_REQUESTED로 남아있어야 함
                    assertThat(rolledBackPayment.getStatus()).isEqualTo(PaymentStatus.CANCEL_REQUESTED);
                });
    }

    @Test
    @DisplayName("실패 시나리오: 이미 '취소 처리 중'인 결제에 대해 중복 취소 요청 시 409 Conflict 에러가 발생한다")
    void deletePayment_Fails_When_StatusIsCancelRequested() throws Exception {
        reservation.setStatus(ReservationStatus.COMPLETED);
        reservationRepository.save(reservation);

        Payment payment = Payment.builder()
                .reservation(reservation)
                .member(loginedMember)
                .status(PaymentStatus.CANCEL_REQUESTED)
                .merchantUid("duplicate-test-uid-" + System.currentTimeMillis())
                .paymentKey("duplicate-test-key")
                .amount(reservation.getPrice())
                .build();
        paymentRepository.save(payment);

        ResultActions resultActions = mvc.perform(
                delete("/api/payment/{reservationId}", reservation.getId())
                        .header("Authorization", "Bearer " + token)
        ).andDo(print());

        resultActions
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("409-4"))
                .andExpect(jsonPath("$.msg").value("이미 취소 처리중인 결제입니다."));
    }

    @Test
    @DisplayName("실패 시나리오: 이미 '취소 완료'된 결제에 대해 중복 취소 요청 시 409 Conflict 에러가 발생한다")
    void deletePayment_Fails_When_StatusIsCancelled() throws Exception {
        reservation.setStatus(ReservationStatus.COMPLETED);
        reservationRepository.save(reservation);

        Payment payment = Payment.builder()
                .reservation(reservation)
                .member(loginedMember)
                .status(PaymentStatus.CANCELLED)
                .merchantUid("cancelled-test-uid-" + System.currentTimeMillis())
                .paymentKey("cancelled-test-key")
                .amount(reservation.getPrice())
                .build();
        paymentRepository.save(payment);

        ResultActions resultActions = mvc.perform(
                delete("/api/payment/{reservationId}", reservation.getId())
                        .header("Authorization", "Bearer " + token)
        ).andDo(print());

        resultActions
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("409-2"))
                .andExpect(jsonPath("$.msg").value("이미 취소된 결제입니다."));
    }

}
