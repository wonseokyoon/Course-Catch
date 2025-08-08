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
    private String nickname;
    @Column(unique = true)
    private String email;
    private String profileImageUrl;

    // todo: 이메일,비밀번호 변경 같은 로직 수행 시 사용
    private boolean isEmailVerified;    // 이메일 인증 여부(기본값 false)
    private boolean deleteFlag;     // 회원 탈퇴 플래그(기본값 false)

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

    public void update(String nickname) {
        this.nickname = nickname;
    }

    // 기본 프사 설정
    public String getProfileImageUrl() {
        return profileImageUrl == null || profileImageUrl.isBlank() ? "https://i.imgur.com/wcxGofh.png" : profileImageUrl;
    }

}