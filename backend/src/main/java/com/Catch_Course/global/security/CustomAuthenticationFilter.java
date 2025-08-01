package com.Catch_Course.global.security;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.member.service.MemberService;
import com.Catch_Course.global.Rq;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationFilter extends OncePerRequestFilter {
    private final Rq rq;
    private final MemberService memberService;

    private boolean isAuthorizationHeader(String header) {

        if (header == null) {
            return false;
        }

        return header.startsWith("Bearer ");
    }

    record AuthToken(String apiKey, String accessToken) {
    }

    // 요청으로부터 authToken(apiKey,accessToken) 꺼냄
    private AuthToken getAuthTokenFromRequest() {
        String header = rq.getHeader("Authorization");

        if (isAuthorizationHeader(header)) {
            String authToken = header.substring("Bearer ".length());
            String[] tokens = authToken.split(" ", 2);

            // 뭐가 하나 없는 경우
            if (tokens.length < 2) {
                return null;
            }

            return new AuthToken(tokens[0], tokens[1]);
        }

        // 쿠키로 체크
        String accessToken = rq.getValueFromCookie("accessToken");
        String apiKey = rq.getValueFromCookie("apiKey");

        if (accessToken == null && apiKey == null) {
            return null;
        }

        return new AuthToken(apiKey, accessToken);
    }

    // accessToken 재발급
    public Member refreshAccessToken(String accessToken, String apiKey) {
        Optional<Member> optionalMember = memberService.findMemberByAccessToken(accessToken);

        // accessToken 에 해당되는 유저가 존재하면
        if (optionalMember.isPresent()) {
            return optionalMember.get();
        }

        // apiKey 값으로 재발급 체크
        Optional<Member> reissueMember = memberService.findByApiKey(apiKey);

        // 그마저도 없으면 넘김
        if (reissueMember.isEmpty()) {
            return null;
        }

        // 새로운 토큰 발급
        String newAccessToken = memberService.getAccessToken(reissueMember.get());

        // 쿠키에 추가
        rq.addCookie("accessToken", newAccessToken);
        rq.addCookie("apiKey", apiKey);

        return reissueMember.get();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // 인증 정보가 필요없는 요청은 통과 (다음 필터에 맞김, 로그인과 회원가입은 없어도 상관없는데 메서드를 명확하게 하기 위해 포함)
        // 다음 필터에서: SecurityConfig 의 permitAll() 조건을 확인하고 인증 없이 통과시켜줌
        String url = request.getRequestURI();

        if (List.of("/api/members/login", "/api/members/join", "/api/members/logout").contains(url)) {
            filterChain.doFilter(request, response);
            return;
        }

        AuthToken tokens = getAuthTokenFromRequest();

        if (tokens == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = tokens.apiKey;
        String accessToken = tokens.accessToken;

        Member member = refreshAccessToken(accessToken, apiKey);

        if (member == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 로그인
        rq.setLogin(member);

        filterChain.doFilter(request, response);
    }
}
