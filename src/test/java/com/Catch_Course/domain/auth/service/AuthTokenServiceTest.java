package com.Catch_Course.domain.auth.service;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.member.service.MemberService;
import com.Catch_Course.global.util.Ut;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class AuthTokenServiceTest {

//    private String secretKey;
    SecretKey secretKey = Keys.hmacShaKeyFor("abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz1234567890".getBytes());
//    private int expireSeconds;

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
        int expireSeconds = 60 * 60 * 24 * 365;
        Map<String,Object> originPayLoad = Map.of("name", "john", "age", 23);

        String jwt = Ut.Jwt.createAccessToken(secretKey, expireSeconds, Map.of("name", "john", "age", 23));
        assertThat(jwt).isNotBlank();

        // JWT 파싱
        Jwt<?,?> parsedJwt = Jwts
                .parser()
                .verifyWith(secretKey)
                .build()
                .parse(jwt);

        Map<String,Object> payLoad = (Map<String, Object>) parsedJwt.getPayload();

        // 원래(암호화 이전) 페이로드와 파싱된 페이로드가 일치하는지 검증
        assertThat(payLoad).containsAllEntriesOf(originPayLoad);
    }

    @Test
    @DisplayName("accessToken 생성")
    void createAccessToken() {
        Member member = memberService.findByUsername("user1").get();
        String accessToken = authTokenService.getAccessToken(member);

        assertThat(accessToken).isNotBlank();
        System.out.println("accessToken = " + accessToken);
    }

    @Test
    @DisplayName("토큰 유효 검증")
    void checkValidToken() {
        Member member = memberService.findByUsername("user1").get();
        String accessToken = authTokenService.getAccessToken(member);

        boolean isValid = Ut.isValidToken(secretKey, accessToken);
        assertThat(isValid).isTrue();
    }


}