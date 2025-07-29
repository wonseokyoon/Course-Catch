package com.Catch_Course.domain.auth.service;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.global.util.Ut;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

    @Value("${custom.jwt.secret-key}")
    private String secretKey;

    @Value("${custom.jwt.expire-seconds}")
    private int expireSeconds;

    // Member의 AccessToken 가져오기
    public String getAccessToken(Member member) {
        Map<String, Object> claims = Map.of("id",member.getId(),"username",member.getUsername());
        return Ut.Jwt.createAccessToken(secretKey, expireSeconds, claims);
    }

    public Map<String, Object> getPayload(String secretKey, String token) {
        Map<String, Object> payload = Ut.Jwt.getPayload(secretKey, token);

        if(payload == null) return null;

        // Json은 Long 표현 못함
        // 역직렬화 과정에서 Long -> double로 변환되는 문제 해결
        Number numberId = (Number) payload.get("id");
        Long id = numberId.longValue();
        String username = (String) payload.get("username");

        return Map.of("id", id, "username", username);
    }

}
