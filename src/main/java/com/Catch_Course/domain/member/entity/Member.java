package com.Catch_Course.domain.member.entity;

import com.Catch_Course.global.entity.BaseTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
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
}