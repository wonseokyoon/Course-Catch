package com.Catch_Course.domain.member.controller;

import com.Catch_Course.domain.email.dto.TempMemberInfo;
import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.member.service.MemberService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class EmailControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberService memberService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private String token;
    private Member loginedMember;

    private static final String RESTORE_PREFIX = "restore ";

    // Redis 컨테이너 생성 및 포트 설정
    @Container
    private static final GenericContainer<?> REDIS_CONTAINER =
            new GenericContainer<>("redis:6-alpine")
                    .withExposedPorts(6379);

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

    private ResultActions sendCodeRequest(String username, String password, String nickname, String email, String profileImageUrl) throws Exception {
        Map<String, String> requestBody = Map.of("username", username, "password", password, "nickname", nickname, "email", email, "profileImageUrl", profileImageUrl);

        // Map -> Json 변환
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(requestBody);

        return mvc
                .perform(
                        post("/api/members/send-code")
                                .content(json)
                                .contentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
                ).andDo(print());
    }

    private ResultActions verifyAndJoinRequest(String email, String verificationCode) throws Exception {
        Map<String, String> requestBody = Map.of("email", email, "verificationCode", verificationCode);

        // Map -> Json 변환
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(requestBody);

        return mvc
                .perform(
                        post("/api/members/verify-code")
                                .content(json)
                                .contentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
                ).andDo(print());
    }

    @Test
    @DisplayName("회원 가입 1단계 - 인증 번호 발송")
    void joinAndSendMail() throws Exception {
        String username = "newUser1";
        String password = "password";
        String nickname = "newNickname1";
        String email = "newEmail1@example.com";
        String profileImageUrl = "newProfileImageUrl";

        ResultActions resultActions = sendCodeRequest(username, password, nickname, email, profileImageUrl);

        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("201-1"))
                .andExpect(jsonPath("$.msg").value("인증 코드가 메일로 전송되었습니다."));
    }

    @Test
    @DisplayName("회원 가입 1단계 실패 - 이미 존재하는 이메일")
    void joinAndSendMail2() throws Exception {
        String username = "newUser1";
        String password = "password";
        String nickname = "newNickname1";
        String email = "newEmail1@example.com";
        String profileImageUrl = "newProfileImageUrl";

        // 회원 가입
        memberService.join(username, password, nickname, email, profileImageUrl);

        String username2 = "newUser2";
        String password2 = "password2";
        String nickname2 = "newNickname2";
        String email2 = "newEmail1@example.com";        // 이미 존재하는 이메일
        String profileImageUrl2 = "newProfileImageUrl2";

        ResultActions resultActions = sendCodeRequest(username2, password2, nickname2, email2, profileImageUrl2);

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400-2"))
                .andExpect(jsonPath("$.msg").value("중복된 이메일입니다."));
    }

    @Test
    @DisplayName("회원 가입 1단계 실패 - 이미 존재하는 아이디")
    void joinAndSendMail3() throws Exception {
        String username = "newUser1";
        String password = "password";
        String nickname = "newNickname1";
        String email = "newEmail1@example.com";
        String profileImageUrl = "newProfileImageUrl";

        // 회원 가입
        memberService.join(username, password, nickname, email, profileImageUrl);

        String username2 = "newUser1";          // 이미 존재하는 아이디
        String password2 = "password2";
        String nickname2 = "newNickname2";
        String email2 = "newEmail2@example.com";
        String profileImageUrl2 = "newProfileImageUrl2";

        ResultActions resultActions = sendCodeRequest(username2, password2, nickname2, email2, profileImageUrl2);

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400-1"))
                .andExpect(jsonPath("$.msg").value("중복된 아이디입니다."));
    }


    @Test
    @DisplayName("회원 가입 2단계 - 인증 번호 검증과 회원 정보 생성")
    void joinAndVerification() throws Exception {
        String username = "newUser1";
        String password = "password";
        String nickname = "newNickname1";
        String email = "newEmail1@example.com";
        String profileImageUrl = "newProfileImageUrl";

        // 회원가입 1단계
        sendCodeRequest(username, password, nickname, email, profileImageUrl);

        // 인증번호를 가져오는 로직
        Object object = redisTemplate.opsForValue().get(email);
        TempMemberInfo info = (TempMemberInfo) object;

        ResultActions resultActions = verifyAndJoinRequest(email, info.getVerificationCode());
        Member member = memberService.findByUsername(username).get();
        assertThat(member.getNickname()).isEqualTo(nickname);

        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("201-2"))
                .andExpect(jsonPath("$.msg").value("인증이 완료되었습니다. 회원가입을 축하합니다."));
    }

    @Test
    @DisplayName("회원 가입 2단계 실패 - 유효하지 않은 인증 요청(잘못된 메일이나, 인증 시간이 지난 메일)")
    void joinAndVerification2() throws Exception {
        String username = "newUser1";
        String password = "password";
        String nickname = "newNickname1";
        String email = "newEmail1@example.com";
        String profileImageUrl = "newProfileImageUrl";

        // 회원가입 1단계
        sendCodeRequest(username, password, nickname, email, profileImageUrl);

        // 인증번호를 가져오는 로직
        Object object = redisTemplate.opsForValue().get(email);
        // 임시 정보는 생성이 되었어야함
        assertThat(object).isNotNull();
        TempMemberInfo info = (TempMemberInfo) object;

        // 잘못된 이메일이 입력
        ResultActions resultActions = verifyAndJoinRequest("incorrectEmail@example.com", info.getVerificationCode());

        resultActions
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401-4"))
                .andExpect(jsonPath("$.msg").value("유효하지 않은 인증 요청입니다."));
    }

    @Test
    @DisplayName("회원 가입 2단계 실패 - 잘못된 인증 코드(틀렸거나, 재전송했을때 이전 인증 코드 입력)")
    void joinAndVerification3() throws Exception {
        String username = "newUser1";
        String password = "password";
        String nickname = "newNickname1";
        String email = "newEmail1@example.com";
        String profileImageUrl = "newProfileImageUrl";

        // 회원가입 1단계
        sendCodeRequest(username, password, nickname, email, profileImageUrl);

        // 인증번호를 가져오는 로직
        Object object = redisTemplate.opsForValue().get(email);
        // 임시 정보는 생성이 되었어야함
        assertThat(object).isNotNull();

        // 잘못된 인증 코드 입력
        ResultActions resultActions = verifyAndJoinRequest(email, "incorrectCode");

        resultActions
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401-5"))
                .andExpect(jsonPath("$.msg").value("잘못된 인증 코드입니다."));
    }

    private ResultActions restoreAndSendRequest(String email) throws Exception {
        Map<String, String> requestBody = Map.of("email", email);

        // Map -> Json 변환
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(requestBody);

        return mvc
                .perform(
                        post("/api/members/restore-send")
                                .content(json)
                                .contentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
                ).andDo(print());
    }

    private ResultActions withdrawRequest(String accessToken) throws Exception {
        return mvc
                .perform(
                        delete("/api/members/withdraw")
                                .header("Authorization", "Bearer " + accessToken)
                ).andDo(print());
    }

    @Test
    @DisplayName("계정 복원 1단계 - 메일 전송")
    void restoreAndSend() throws Exception {
        withdrawRequest(token);     // 계정 삭제
        String email = "user1@example.com";
        ResultActions resultActions = restoreAndSendRequest(email);

        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("201-1"))
                .andExpect(jsonPath("$.msg").value("인증 코드가 메일로 전송되었습니다."));
    }

    private ResultActions restoreAndVerificationRequest(String email, String verificationCode) throws Exception {
        Map<String, String> requestBody = Map.of("email", email, "verificationCode", verificationCode);

        // Map -> Json 변환
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(requestBody);

        return mvc
                .perform(
                        post("/api/members/restore-verify")
                                .content(json)
                                .contentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
                ).andDo(print());
    }

    @Test
    @DisplayName("계정 복원 2단계 - 인증")
    void restoreAndVerification() throws Exception {
        withdrawRequest(token);     // 계정 삭제
        String email = "user1@example.com";
        String username = "user1";

        // 메일 전송
        restoreAndSendRequest(email);

        // 인증번호
        String verificationCode = (String) redisTemplate.opsForValue().get(RESTORE_PREFIX + email);
        ResultActions resultActions = restoreAndVerificationRequest(email, verificationCode);

        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("201-3"))
                .andExpect(jsonPath("$.msg").value("계정이 복구되었습니다."));


        // 삭제 플래그 false
        Member member = memberService.findByUsernameAll(username).get();
        assertThat(member.isDeleteFlag()).isFalse();
    }

    @Test
    @DisplayName("계정 복원 2단계 실패 - 복구 가능한 메일이 아닌 경우")
    void restoreAndVerification2() throws Exception {
        String email = "user1@example.com";
        Member member = memberService.findByEmail(email).get();
        memberService.deleteMember(member);     // 하드 삭제

        // 메일 전송
        restoreAndSendRequest(email);

        // 인증번호
        String verificationCode = (String) redisTemplate.opsForValue().get(RESTORE_PREFIX + email);
        ResultActions resultActions = restoreAndVerificationRequest(email, verificationCode);

        resultActions
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("403-2"))
                .andExpect(jsonPath("$.msg").value("복구 가능한 메일이 아닙니다."));
    }

    @Test
    @DisplayName("계정 복원 2단계 실패 - 인증 코드 만료")
    void restoreAndVerification3() throws Exception {
        String email = "user1@example.com";

        // 메일 전송
        restoreAndSendRequest(email);

        // Redis에서 삭제
        String verificationCode = (String) redisTemplate.opsForValue().get(RESTORE_PREFIX + email);
        redisTemplate.delete(RESTORE_PREFIX + email);
//        redisTemplate.opsForValue().set(RESTORE_PREFIX + email, "expired");
        ResultActions resultActions = restoreAndVerificationRequest(email, verificationCode);

        resultActions
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401-4"))
                .andExpect(jsonPath("$.msg").value("유효하지 않은 인증 요청입니다."));
    }

    @Test
    @DisplayName("계정 복원 2단계 실패 - 잘못된 인증 코드")
    void restoreAndVerification4() throws Exception {
        String email = "user1@example.com";

        // 메일 전송
        restoreAndSendRequest(email);

        // Redis에서 삭제
        String verificationCode = (String) redisTemplate.opsForValue().get(RESTORE_PREFIX + email);
        redisTemplate.opsForValue().set(RESTORE_PREFIX + email, "incorrectCode");
        ResultActions resultActions = restoreAndVerificationRequest(email, verificationCode);

        resultActions
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401-5"))
                .andExpect(jsonPath("$.msg").value("잘못된 인증 코드입니다."));
    }

}
