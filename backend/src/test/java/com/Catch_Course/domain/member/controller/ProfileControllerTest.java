package com.Catch_Course.domain.member.controller;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.member.service.MemberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@Testcontainers
public class ProfileControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberService memberService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String token;
    private Member loginedMember;

    @BeforeEach
    @DisplayName("user1로 로그인 셋업 + Redis 임베디드 서버 시작")
    void setUp() {
        loginedMember = memberService.findByUsername("user1").get();
        token = memberService.getAuthToken(loginedMember);
    }

    private ResultActions meRequest(String accessToken) throws Exception {
        return mvc
                .perform(
                        get("/api/profile/me")
                                .header("Authorization", "Bearer " + accessToken)
                ).andDo(print());
    }

    @Test
    @DisplayName("내 정보 조회 - accessToken")
    void me() throws Exception {
        ResultActions resultActions = meRequest(token);

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("내 정보 조회가 완료되었습니다."));

    }

    @Test
    @DisplayName("내 정보 조회 - 만료된 토큰으로 재발급 확인")
    void me2() throws Exception {
        String expiredToken
                = "expiredToken";
        String refreshToken = memberService.getRefreshToken(loginedMember);
        ResultActions resultActions = meRequest(expiredToken+" "+refreshToken);

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("내 정보 조회가 완료되었습니다."));

    }

    private ResultActions updateProfileRequest(String accessToken, String nickname, String newPassword, String email, String profileImageUrl, MockHttpSession session) throws Exception {
        Map<String, String> requestBody = Map.of(
                "nickname", nickname,
                "newPassword", newPassword,
                "email", email,
                "profileImageUrl", profileImageUrl
        );

        // Map -> Json 변환
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(requestBody);

        return mvc
                .perform(
                        put("/api/profile/me")
                                .header("Authorization", "Bearer " + accessToken)
                                .content(json)
                                .contentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
                                .session(session)   // 비밀번호 인증과 같은 세션 공유
                ).andDo(print());
    }

    @Test
    @DisplayName("프로필 수정")
    void updateProfile() throws Exception {
        String nickname = "newNickname";
        String password = "user11234";
        String newPassword = "newPassword";
        String email = "newEmail@example.com";
        String profileImageUrl = "newProfileImageUrl";

        // 1. 세션 객체 생성
        MockHttpSession session = new MockHttpSession();

        // 비밀번호 인증
        verifyPasswordRequest(token, password, session)
                .andExpect(status().isOk());

        ResultActions resultActions = updateProfileRequest(token, nickname, newPassword, email, profileImageUrl, session);

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("프로필 수정이 완료되었습니다. 다시 로그인 해주세요."))
                .andExpect(jsonPath("$.data.nickname").value(nickname))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.profileImageUrl").value(profileImageUrl));

        // DB 조회해서 비밀번호 비교
        Member member = memberService.findByEmail(email).get();
        passwordEncoder.matches(newPassword, member.getPassword());
    }

    private ResultActions verifyPasswordRequest(String accessToken, String password, MockHttpSession session) throws Exception {
        Map<String, String> requestBody = Map.of("password", password);

        // Map -> Json 변환
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(requestBody);

        return mvc
                .perform(
                        post("/api/profile/verify-password")
                                .header("Authorization", "Bearer " + accessToken)
                                .content(json)
                                .contentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
                                .session(session)   // 세션 객체 추가
                ).andDo(print());
    }

    @Test
    @DisplayName("비밀번호 인증")
    void verifyPassword() throws Exception {
        String password = "user11234";
        MockHttpSession session = new MockHttpSession();

        ResultActions resultActions = verifyPasswordRequest(token, password, session);

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("인증되었습니다."));
    }

    @Test
    @DisplayName("비밀번호 인증 실패")
    void verifyPassword2() throws Exception {
        String password = "incorrectPassword";
        MockHttpSession session = new MockHttpSession();

        ResultActions resultActions = verifyPasswordRequest(token, password, session);

        resultActions
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("403-3"))
                .andExpect(jsonPath("$.msg").value("비밀번호가 올바르지 않습니다."));
    }

    @Test
    @DisplayName("프로필 수정 실패 - 비밀번호 인증 실패")
    void updateProfile2() throws Exception {
        String nickname = "newNickname";
        String newPassword = "newPassword";
        String email = "newEmail@example.com";
        String profileImageUrl = "newProfileImageUrl";

        // 1. 세션 객체 생성
        MockHttpSession session = new MockHttpSession();

        ResultActions resultActions = updateProfileRequest(token, nickname, newPassword, email, profileImageUrl, session);

        resultActions
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("403-4"))
                .andExpect(jsonPath("$.msg").value("비밀번호 인증이 필요합니다."));
    }
}
