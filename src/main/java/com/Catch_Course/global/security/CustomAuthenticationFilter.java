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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");

        // 인증 값이 유효할때
        if(authorization != null && authorization.startsWith("Bearer ")) {
//            String apiKey = authorization.substring("Bearer ".length());
//            Optional<Member> optionalMember = memberService.findByApiKey(apiKey);

            String accessToken = authorization.substring("Bearer ".length());
            Optional<Member> optionalMember = memberService.findByAccessToken(accessToken);

            // 키 값에 해당되는 유저가 존재하면
            if(optionalMember.isPresent()) {
                Member member = optionalMember.get();
                // 로그인
                rq.setLogin(member.getUsername());
            }
        }

        filterChain.doFilter(request, response);
    }
}