package com.Catch_Course.domain.member.entity;

import com.Catch_Course.global.entity.BaseTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EntityListeners(AuditingEntityListener.class)
public class Member extends BaseTime {

    @Column(length = 100, unique = true)
    private String username;
    @Column(length = 100)
    private String password;
    @Column(length = 100, unique = true)
    private String apiKey;
    @Column(length = 100, unique = true)
    private String nickname;

    public boolean isAdmin() {
        return username.equals("admin");
    }

    // 권한 반환
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return getAuthoritiesString()
                .stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());      // 가변 리스트로 수정 가능성 열어둠
    }

    // 권한(authorities)은 처음 생성시 empty 리스트
    public List<String> getAuthoritiesString() {
        List<String> authorities = new ArrayList<>();

        // 관리자 -> 앞에 관리자 권한 표시 추가
        if(isAdmin()) {
            authorities.add("ROLE_ADMIN");
        }
        // 굳이 안해도 될거같음
//        else{
//            authorities.add("ROLE_USER");
//        }

        return authorities;
    }
}