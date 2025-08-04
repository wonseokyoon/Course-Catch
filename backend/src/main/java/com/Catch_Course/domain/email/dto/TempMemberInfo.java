package com.Catch_Course.domain.email.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class TempMemberInfo {
    private String username;
    private String password;
    private String nickname;
    private String email;
    private String profileImageUrl;
    private LocalDateTime createdAt;
    private String verificationCode;

    public TempMemberInfo(String username, String password, String nickname, String email, String profileImageUrl, String verificationCode) {
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.verificationCode = verificationCode;
        this.createdAt = LocalDateTime.now();
    }
}
