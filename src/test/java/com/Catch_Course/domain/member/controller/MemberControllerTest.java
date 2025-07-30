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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class MemberControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberService memberService;

    private String token;
    private Member loginedMember;

    @BeforeEach
    @DisplayName("user1로 로그인 셋업")
    void setUp() {
        loginedMember = memberService.findByUsername("user1").get();
        token = memberService.getAuthToken(loginedMember);
        System.out.println("token: "+ token);
        System.out.println("=============셋업=============");
    }

    private ResultActions joinRequest(String username,String password, String nickname) throws Exception {
        Map<String,String> requestBody = Map.of("username", username, "password", password, "nickname", nickname);

        // Map -> Json 변환
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(requestBody);

        return mvc
                .perform(
                        post("/api/members/join")
                                .content(json)
                                .contentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
                ).andDo(print());
    }

    @Test
    @DisplayName("회원 가입")
    void join() throws Exception {
        String username = "newUser1";
        String password = "password";
        String nickname = "newNickname1";

        ResultActions resultActions = joinRequest(username,password,nickname);
        Member member = memberService.findByUsername(username).get();

        assertThat(member.getNickname()).isEqualTo(nickname);

        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("201-1"))
                .andExpect(jsonPath("$.msg").value("회원 가입이 완료되었습니다."));
    }

    private ResultActions loginRequest(String username,String password) throws Exception {
        Map<String,String> requestBody = Map.of("username", username, "password", password);

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

        ResultActions resultActions = loginRequest(username,password);
        Member member = memberService.findByUsername(username).get();

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("%s님 환영합니다.".formatted(member.getNickname())))
                .andExpect(jsonPath("$.data.memberDto.id").value(member.getId()))    // id 검증
                .andExpect(jsonPath("$.data.memberDto.nickname").value(member.getNickname()))    // 닉네임 검증
                .andExpect(jsonPath("$.data.apiKey").value(member.getApiKey()))    // apiKey 검증
                .andExpect(jsonPath("$.data.accessToken").exists());    // accessToken 나오는지
    }

    private ResultActions meRequest(String accessToken) throws Exception {
        return mvc
                .perform(
                        get("/api/members/me")
                                .header("Authorization", "Bearer " + accessToken)
                ).andDo(print());
    }


    @Test
    @DisplayName("내 정보 조회 - accessToken")
    void  me() throws Exception {
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
                = "user1 eyJhbGciOiJIUzUxMiJ9.eyJ1c2VybmFtZSI6InVzZXIxIiwiaWQiOjMsImlhdCI6MTc1Mzg1NjQ2OSwiZXhwIjoxNzUzODU2NDc0fQ.-90YTIv40Mcdx5WCL2lbGnuXErcdOnBSQAyrKx42rdjurZdJvOSm8w_JxE9IvLxjE0HKss985XmXTmoRUrUv2g";

        ResultActions resultActions = meRequest(expiredToken);

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("내 정보 조회가 완료되었습니다."));

    }
}