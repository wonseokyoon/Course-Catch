package com.Catch_Course.domain.auth.service;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.global.util.Ut;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

    // Member의 AccessToken 가져오기
    public String getAccessToken(Member member) {
        int expireSeconds = 60 * 60 * 24 * 365;
        SecretKey secretKey = Keys.hmacShaKeyFor("abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz1234567890".getBytes());
        Map<String, Object> claims = Map.of("id",member.getId(),"username",member.getUsername());

        return Ut.Jwt.createToken(secretKey, expireSeconds, claims);
    }

}
