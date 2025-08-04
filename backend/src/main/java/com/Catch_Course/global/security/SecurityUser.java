package com.Catch_Course.global.security;

import com.Catch_Course.domain.member.entity.Member;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

public class SecurityUser extends User implements OAuth2User {
    @Getter
    private long id;
    @Getter
    private String nickname;

    // 기존 SpringSecurity 가 요구하는 인증 정보: username, password, authorities
    // id 꼽사리 끼우는 커스터마이징
    public SecurityUser(long id, String username, String password, String nickname, Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.id = id;
        this.nickname = nickname;
    }

    public SecurityUser(Member member) {
        this(member.getId(), member.getUsername(), member.getPassword(), member.getNickname(), member.getAuthorities());
    }

    @Override
    public String getName() {
        return this.getUsername();
    }

    @Override
    public <A> A getAttribute(String name) {
        return OAuth2User.super.getAttribute(name);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return Map.of();
    }
}
