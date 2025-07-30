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

            String token = authorization.substring("Bearer ".length());
            String[] params = token.split(" ",2);

            // 뭐가 하나 없는 경우
            if(params.length < 2) {
                filterChain.doFilter(request, response);
                return;
            }

            String apiKey = params[0];
            String accessToken = params[1];

            Optional<Member> optionalMember = memberService.findMemberByAccessToken(accessToken);

            // accessToken 에 해당되는 유저가 존재하면
            if(optionalMember.isPresent()) {

                Member member = optionalMember.get();
                // 로그인
                rq.setLogin(member);
            } else {

                // apiKey 값으로 재발급 체크
                Optional<Member> reissueMember = memberService.findByApiKey(apiKey);

                // 그마저도 없으면 넘김
                if(reissueMember.isEmpty()) {
                    filterChain.doFilter(request, response);
                    return;
                }

                // 새로운 토큰 발급
                String newToken  = memberService.getAuthToken(reissueMember.get());

                // 헤더에 추가
                response.setHeader("Authorization", "Bearer " + newToken);

                Member member = reissueMember.get();
                rq.setLogin(member);
            }
        }

        filterChain.doFilter(request, response);
    }
}