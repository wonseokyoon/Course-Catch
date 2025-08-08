package com.Catch_Course.global;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.member.service.MemberService;
import com.Catch_Course.global.exception.ServiceException;
import com.Catch_Course.global.security.SecurityUser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Optional;

// Request, Response, Session, Cookie, Header
@Component
@RequiredArgsConstructor
@RequestScope   // 요청마다 값을 생성(값 갱신)
public class Rq {

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final MemberService memberService;

    // 헤더에서 API 키를 직접 읽어서 인증하는 방식
    public Member getAuthenticatedActor() {

        String authorizationValue = request.getHeader("Authorization");
        String authToken = authorizationValue.substring("Bearer ".length());
        String[] tokens = authToken.split(" ", 2);

        String refreshToken = tokens[1];
        Optional<Member> opActor = memberService.findMemberByRefreshToken(refreshToken);

        if (opActor.isEmpty()) {
            throw new ServiceException("401-1", "잘못된 비밀번호 입니다.");
        }

        return opActor.get();
    }

    public void setLogin(Member member) {
        // 유저 정보 생성
        UserDetails user = new SecurityUser(member.getId(), member.getUsername(), "", member.getNickname(), member.getAuthorities());

        // 인증 정보 저장소
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }

    // dummy 멤버 객체 반환
    public Member getDummyMember() {
        // Security 컨텍스트의 사용자 정보를 꺼내옴
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new ServiceException("401-2", "로그인이 필요합니다.");
        }

        // 현재 인증된 사용자 정보
        Object principal = authentication.getPrincipal();

        // 정의한 규격(SecurityUser) 인지 확인
        if (!(principal instanceof SecurityUser user)) {
            throw new ServiceException("401-3", "잘못된 인증 정보 입니다.");
        }

        return Member.builder()
                .id(user.getId())
                .username(user.getUsername())
                .build();
    }

    // 임시(더미) 멤버를 진짜 멤버 객체로 반환
    public Member getMember(Member dummyMember) {
        return memberService.findById(dummyMember.getId()).get();
    }

    public String getHeader(String name) {
        return request.getHeader(name);
    }

    public void setHeader(String name, String value) {
        response.setHeader(name, value);
    }

    public void addCookie(String name, String value) {
        Cookie cookie = new Cookie(name, value);

        cookie.setDomain("localhost");
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setAttribute("SameSite", "Strict");

        response.addCookie(cookie);
    }

    public String getValueFromCookie(String name) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(name)) {
                return cookie.getValue();
            }
        }

        return null;
    }

    public void removeCookie(String name) {
        // 원칙적으로 쿠키를 서버에서 삭제하는것은 불가능
        Cookie cookie = new Cookie(name,null);

        cookie.setDomain("localhost");
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setAttribute("SameSite", "Strict");

        // 즉시만료시킨 쿠키를 덮어씌움
        cookie.setMaxAge(0);

        response.addCookie(cookie);
    }
}
