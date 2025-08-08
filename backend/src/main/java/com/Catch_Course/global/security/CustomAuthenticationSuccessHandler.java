package com.Catch_Course.global.security;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.member.service.MemberService;
import com.Catch_Course.global.Rq;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final MemberService memberService;
    private final Rq rq;
    private final RedisTemplate<String,Object> redisTemplate;

    private static final String REFRESH_PREFIX = "refresh: ";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        HttpSession session = request.getSession();
        String redirectUrl = (String) session.getAttribute("redirectUrl");  // 세션 객체를 가져옴(로그인 전 방문 한 주소)

        if(redirectUrl == null) {   // 바로 로그인 한 경우
            redirectUrl ="http://localhost:3000";   // 리다이렉트 주소를 홈으로 설정
        }

        session.removeAttribute("redirectUrl"); // 기존 세션 주소 삭제(리디렉션 정보 재사용 방지)

        // 쿠키 추가
        Member member = rq.getMember(rq.getDummyMember());
        String accessToken = memberService.getAccessToken(member);
        String refreshToken = (String) redisTemplate.opsForValue().get(REFRESH_PREFIX+member.getUsername());

        rq.addCookie("accessToken", accessToken);
        rq.addCookie("refreshToken", refreshToken);

        response.sendRedirect(redirectUrl);     // 리다이렉트
    }
}
