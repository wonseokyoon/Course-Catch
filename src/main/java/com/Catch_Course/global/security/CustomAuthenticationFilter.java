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
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationFilter extends OncePerRequestFilter {
    private final Rq rq;
    private final MemberService memberService;

    private boolean isAuthorizationHeader(String header) {

        if(header == null){
            return false;
        }

        return header.startsWith("Bearer ");
    }

    // 요청으로부터 authToken(apiKey,accessToken) 꺼냄
    private String[] getAuthTokenFromRequest() {
        String header = rq.getHeader("Authorization");

        if(isAuthorizationHeader(header)) {
            String authToken = header.substring("Bearer ".length());
            String[] tokens = authToken.split(" ",2);

            // 뭐가 하나 없는 경우
            if(tokens.length < 2) {
                return null;
            }

            return tokens;
        } else {
            // 헤더 인증 값이 유효하지 않을때
            // 쿠키로 체크
            String accessToken = rq.getValueFromCookie("accessToken");
            String apiKey = rq.getValueFromCookie("apiKey");

            if(accessToken == null || apiKey == null) {
                return null;
            }

            return new String[]{apiKey, accessToken};
        }
    }

    // accessToken 재발급
    public Member refreshAccessToken(String accessToken, String apiKey) {
        Optional<Member> optionalMember = memberService.findMemberByAccessToken(accessToken);

        // accessToken 에 해당되는 유저가 존재하면
        if(optionalMember.isPresent()) {
            return optionalMember.get();
        } else {

            // apiKey 값으로 재발급 체크
            Optional<Member> reissueMember = memberService.findByApiKey(apiKey);

            // 그마저도 없으면 넘김
            if(reissueMember.isEmpty()) {
                return null;
            }

            // 새로운 토큰 발급
            String newAccessToken  = memberService.getAccessToken(reissueMember.get());

            // 헤더에 추가
            rq.setHeader("Authorization", "Bearer " + newAccessToken);
            // 쿠키에 추가
            rq.addCookie("accessToken", newAccessToken);

            return reissueMember.get();
        }

    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String[] tokens = getAuthTokenFromRequest();

        if(tokens == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = tokens[0];
        String accessToken = tokens[1];

        Member member = refreshAccessToken(accessToken, apiKey);

        if(member == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 로그인
        rq.setLogin(member);

        filterChain.doFilter(request, response);
    }
}
