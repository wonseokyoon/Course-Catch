package com.Catch_Course.global.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

public class SecurityUser extends User {
    @Getter
    private Long id;

    // 기존 SpringSecurity 가 요구하는 인증 정보: username, password, authorities
    // id 꼽사리 끼우는 커스터마이징
    public SecurityUser(Long id, String username, String password, Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.id = id;
    }
}
