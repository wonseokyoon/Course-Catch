package com.Catch_Course.domain.member.service;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.global.util.Ut;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class AuthTokenServiceTest {

    @Value("${custom.jwt.secret-key}")
    private String secretKey;

    @Value("${custom.jwt.expire-seconds}")
    private int expireSeconds;

    @Autowired
    private AuthTokenService authTokenService;

    @Autowired
    private MemberService memberService;

    @Test
    @DisplayName("AuthTokenService 생성")
    void init(){
        assertThat(authTokenService).isNotNull();
    }

    @Test
    @DisplayName("jwt 생성")
    void createToken() {
        Map<String,Object> originPayLoad = Map.of("id", 1L, "username", "john");

        String jwt = Ut.Jwt.createAccessToken(secretKey, expireSeconds, Map.of("id", 1L, "username", "john"));
        assertThat(jwt).isNotBlank();

        Map<String,Object> payload = authTokenService.getPayload(secretKey, jwt);

        // 원래(암호화 이전) 페이로드와 파싱된 페이로드가 일치하는지 검증
        assertThat(payload).containsAllEntriesOf(originPayLoad);
    }

    @Test
    @DisplayName("accessToken 생성")
    void createAccessToken() {
        Member member = memberService.findByUsername("user1").get();
        String accessToken = authTokenService.createAccessToken(member);

        assertThat(accessToken).isNotBlank();
        System.out.println("accessToken = " + accessToken);
    }

    @Test
    @DisplayName("토큰 유효 검증")
    void checkValidToken() {
        Member member = memberService.findByUsername("user1").get();
        String accessToken = authTokenService.createAccessToken(member);
        boolean isValid = Ut.Jwt.isValidToken(secretKey, accessToken);
        assertThat(isValid).isTrue();

        Map<String, Object> payload = authTokenService.getPayload(secretKey, accessToken);
        System.out.println("payload: "+payload);

        assertThat(payload).containsAllEntriesOf(
                Map.of("id", member.getId(), "username", member.getUsername())
        );
    }


}