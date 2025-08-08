package com.Catch_Course.global.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

public class Ut {
    public static class Json {

        private static final ObjectMapper objectMapper = new ObjectMapper();

        public static String toString(Object obj) {
            try {
                return objectMapper.writeValueAsString(obj);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class Jwt {
        public static String createAccessToken(String secretKey, int expireSeconds, Map<String, Object> claims) {
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());

            Date issuedAt = new Date();
            Date expiration = new Date(issuedAt.getTime() + 1000L * expireSeconds);

            return Jwts.builder()
                    .claims(claims)
                    .issuedAt(issuedAt)
                    .expiration(expiration)
                    .signWith(key)
                    .compact();
        }

        public static String createRefreshToken(String secretKey, int expireSeconds, Map<String, Object> claims) {
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());

            Date issuedAt = new Date();
            Date expiration = new Date(issuedAt.getTime() + 1000L * expireSeconds);

            return Jwts.builder()
                    .claims(claims)
                    .issuedAt(issuedAt)
                    .expiration(expiration)
                    .signWith(key)
                    .compact();
        }

        // 토큰이 유효한지 검증
        public static boolean isValidToken(String secretKey, String token) {
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());
            try {
                Jwts
                        .parser()
                        .verifyWith(key)
                        .build()
                        .parse(token);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

            return true;
        }

        // payload 가져오기
        public static Map<String, Object> getPayload(String secretKey, String token) {
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());

            return (Map<String, Object>) Jwts
                    .parser()
                    .verifyWith(key)
                    .build()
                    .parse(token)
                    .getPayload();
        }
    }
}