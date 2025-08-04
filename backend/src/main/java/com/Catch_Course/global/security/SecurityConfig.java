package com.Catch_Course.global.security;

import com.Catch_Course.global.dto.RsData;
import com.Catch_Course.global.util.Ut;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAuthenticationFilter customAuthenticationFilter;
    private final CustomAuthorizationRequestResolver customAuthorizationRequestResolver;

    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authorizeHttpRequests) ->
                        authorizeHttpRequests
                                // swagger,h2 허용
                                .requestMatchers("/swagger-ui/**", "/h2-console/**", "/v3/api-docs/**")
                                .permitAll()
                                .requestMatchers("/api/courses/statistics")
                                .hasRole("ADMIN")
                                .requestMatchers(HttpMethod.GET, "/api/**")
                                .permitAll()
                                .requestMatchers("/api/members/login", "/api/members/join","/api/members/logout")
                                .permitAll()
                                .anyRequest()
                                .authenticated()
                )
                .headers((headers) -> headers
                        .addHeaderWriter(new XFrameOptionsHeaderWriter(
                                XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN)))

                // csrf 비활성화
                .csrf(csrf -> csrf.disable())
                // CORS
                .cors(cors -> {})
                .oauth2Login(oauth2->{
                    oauth2.authorizationEndpoint(
                            authorizationEndpoint -> authorizationEndpoint
                                    .authorizationRequestResolver(customAuthorizationRequestResolver)
                    );
                    oauth2.successHandler((request, response, authentication) -> {
                        HttpSession session = request.getSession();
                        String redirectUrl = (String) session.getAttribute("redirectUrl");  // 세션 객체를 가져옴(로그인 전 방문 한 주소)
                        if(redirectUrl == null) {   // 바로 로그인 한 경우
                            redirectUrl ="http://localhost:3000";   // 리다이렉트 주소를 홈으로 설정
                        }
                        session.removeAttribute("redirectUrl"); // 기존 세션 주소 삭제(리디렉션 정보 재사용 방지)
                        response.sendRedirect(redirectUrl);
                    });
                })
                .addFilterBefore(customAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // 예외 처리
                .exceptionHandling(
                        exceptionHandling -> exceptionHandling
                                // 인증 실패 예외
                                .authenticationEntryPoint(
                                        (request, response, authException) -> {
                                            response.setContentType("application/json;charset=UTF-8");
                                            response.setStatus(401);
                                            response.getWriter().write(
                                                    Ut.Json.toString(
                                                            new RsData("401-1", "잘못된 인증키입니다.")
                                                    )
                                            );
                                        }
                                )
                                // 접근 권한 예외
                                .accessDeniedHandler(
                                        (request, response, authException) -> {
                                            response.setContentType("application/json;charset=UTF-8");
                                            response.setStatus(403);
                                            response.getWriter().write(
                                                    Ut.Json.toString(
                                                            new RsData("403-1", "접근 권한이 없습니다.")
                                                    )
                                            );
                                        }
                                )
                );
        ;
        return http.build();
    }
}