package com.Catch_Course.domain.member.controller;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.member.service.MemberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@Testcontainers
class MemberControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberService memberService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private String token;
    private Member loginedMember;

    private static final String REFRESH_PREFIX = "refresh: ";

    // Redis 컨테이너 생성 및 포트 설정
    @Container
    private static final GenericContainer<?> REDIS_CONTAINER =
            new GenericContainer<>("redis:6-alpine")
                    .withExposedPorts(6379)
                    .waitingFor(new WaitAllStrategy());

    // RedisTemplate이 컨테이너의 동적 포트를 사용하도록 설정
    @DynamicPropertySource
    static void setRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379));
    }

    @BeforeEach
    @DisplayName("user1로 로그인 셋업 + Redis 임베디드 서버 시작")
    void setUp() {
        loginedMember = memberService.findByUsername("user1").get();
        token = memberService.getAuthToken(loginedMember);
    }

    @AfterEach
    @DisplayName("Redis 데이터 초기화")
    void tearDown() {
        // 각 테스트가 끝난 후 Redis 데이터를 초기화
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    private ResultActions loginRequest(String username, String password) throws Exception {
        Map<String, String> requestBody = Map.of("username", username, "password", password);

        // Map -> Json 변환
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(requestBody);

        return mvc
                .perform(
                        post("/api/members/login")
                                .content(json)
                                .contentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
                ).andDo(print());
    }

    @Test
    @DisplayName("로그인")
    void login() throws Exception {
        String username = "user1";
        String password = "user11234";

        ResultActions resultActions = loginRequest(username, password);
        Member member = memberService.findByUsername(username).get();

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("%s님 환영합니다.".formatted(member.getNickname())))
                .andExpect(jsonPath("$.data.memberDto.id").value(member.getId()))    // id 검증
                .andExpect(jsonPath("$.data.memberDto.nickname").value(member.getNickname()))    // 닉네임 검증
                .andExpect(jsonPath("$.data.memberDto.profileImageUrl").isNotEmpty())    // profileImageUrl notnull 체크
                .andExpect(jsonPath("$.data.memberDto.profileImageUrl").value(member.getProfileImageUrl()))    // 검증
                .andExpect(jsonPath("$.data.refreshToken").value(redisTemplate.opsForValue().get(REFRESH_PREFIX+username)))    // refreshToken 검증
                .andExpect(jsonPath("$.data.accessToken").exists());    // accessToken 나오는지

    }

    private ResultActions logoutRequest(String accessToken) throws Exception {
        return mvc
                .perform(
                        delete("/api/members/logout")
                                .header("Authorization", "Bearer " + accessToken)
                ).andDo(print());
    }

    @Test
    @DisplayName("로그아웃 - 쿠키 삭제")
    void logout() throws Exception {
        ResultActions resultActions = logoutRequest(token);

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("로그아웃이 완료되었습니다."));

        // 쿠키 삭제됐는지 확인
        resultActions
                .andExpect(
                        mvcResult -> {
                            Cookie accessToken = mvcResult.getResponse().getCookie("accessToken");
                            assertThat(accessToken).isNotNull();    // max-age 를 0으로 해서 반환, 이 단계에서 쿠키 객체는 존재함
                            assertThat(accessToken.getMaxAge()).isEqualTo(0);

                            Cookie refreshToken = mvcResult.getResponse().getCookie("refreshToken");
                            assertThat(refreshToken).isNotNull();
                            assertThat(refreshToken.getMaxAge()).isEqualTo(0);
                        }
                );
    }

    private ResultActions withdrawRequest(String accessToken) throws Exception {
        return mvc
                .perform(
                        delete("/api/members/withdraw")
                                .header("Authorization", "Bearer " + accessToken)
                ).andDo(print());
    }

    @Test
    @DisplayName("계정 삭제")
    void withdraw() throws Exception {
        ResultActions resultActions = withdrawRequest(token);

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("회원탈퇴가 완료되었습니다."));

        // 쿠키 삭제됐는지 확인
        resultActions
                .andExpect(
                        mvcResult -> {
                            Cookie accessToken = mvcResult.getResponse().getCookie("accessToken");
                            assertThat(accessToken).isNotNull();    // max-age 를 0으로 해서 반환, 이 단계에서 쿠키 객체는 존재함
                            assertThat(accessToken.getMaxAge()).isEqualTo(0);

                            Cookie refreshToken = mvcResult.getResponse().getCookie("refreshToken");
                            assertThat(refreshToken).isNotNull();
                            assertThat(refreshToken.getMaxAge()).isEqualTo(0);
                        }
                );

        // 삭제 플래그가 참
        Member member = memberService.findByUsernameAll("user1").get();
        assertThat(member.isDeleteFlag()).isTrue();
    }

}
