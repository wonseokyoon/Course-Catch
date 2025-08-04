package com.Catch_Course.global.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        HttpSession session = request.getSession();
        String redirectUrl = (String) session.getAttribute("redirectUrl");  // 세션 객체를 가져옴(로그인 전 방문 한 주소)

        if(redirectUrl == null) {   // 바로 로그인 한 경우
            redirectUrl ="http://localhost:3000";   // 리다이렉트 주소를 홈으로 설정
        }

        session.removeAttribute("redirectUrl"); // 기존 세션 주소 삭제(리디렉션 정보 재사용 방지)
        response.sendRedirect(redirectUrl);     // 리다이렉트
    }

}
