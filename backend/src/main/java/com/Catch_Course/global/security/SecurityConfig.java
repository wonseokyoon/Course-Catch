package com.Catch_Course.global.security;

import com.Catch_Course.global.dto.RsData;
import com.Catch_Course.global.util.Ut;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAuthenticationFilter customAuthenticationFilter;
    private final CustomAuthorizationRequestResolver customAuthorizationRequestResolver;
    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    @Bean
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
                                .requestMatchers("/api/members/login", "/api/members/join","/api/members/logout","/session"
                                ,"/api/members/send-code","/api/members/verify-code")
                                .permitAll()
                                .anyRequest()
                                .authenticated()
                )
                .headers((headers) -> headers
                        .addHeaderWriter(new XFrameOptionsHeaderWriter(
                                XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN)))

                // csrf 비활성화
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sessionManagement -> {
                    sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                })
                // CORS
                .cors(cors -> {})
                .oauth2Login(oauth2->{
                    oauth2.authorizationEndpoint(
                            authorizationEndpoint -> authorizationEndpoint
                                    .authorizationRequestResolver(customAuthorizationRequestResolver)
                    );
                    oauth2.successHandler(customAuthenticationSuccessHandler);
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