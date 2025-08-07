package com.Catch_Course.domain.member.service;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.global.util.Ut;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

    @Value("${custom.jwt.secret-key}")
    private String secretKey;

    @Value("${custom.jwt.expire-seconds}")
    private int expireSeconds;

    @Value("${custom.jwt.refresh-expire-seconds}")
    private int refreshExpireSeconds;

    private final RedisTemplate<String,Object> redisTemplate;

    private static final String REFRESH_PREFIX = "refresh: ";

    // Member의 AccessToken 가져오기
    String createAccessToken(Member member) {
        Map<String, Object> claims = Map.of("id", member.getId(), "username", member.getUsername());
        return Ut.Jwt.createAccessToken(secretKey, expireSeconds, claims);
    }

    String createRefreshToken(Member member) {
        Map<String, Object> claims = Map.of("id", member.getId(), "username", member.getUsername());
        String refreshToken = Ut.Jwt.createRefreshToken(secretKey, refreshExpireSeconds, claims);
        redisTemplate.opsForValue().set(REFRESH_PREFIX + member.getUsername(), refreshToken,refreshExpireSeconds, TimeUnit.SECONDS);
        return refreshToken;
    }

    Map<String, Object> getPayload(String token) {
        if (!Ut.Jwt.isValidToken(secretKey, token)) return null;

        Map<String, Object> payload = Ut.Jwt.getPayload(secretKey, token);

        // Json은 Long 표현 못함
        // 역직렬화 과정에서 Long -> double로 변환되는 문제 해결
        Number numberId = (Number) payload.get("id");
        long id = numberId.longValue();
        String username = (String) payload.get("username");

        return Map.of("id", id, "username", username);
    }

}
