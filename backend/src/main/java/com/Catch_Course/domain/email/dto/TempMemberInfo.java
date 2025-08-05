package com.Catch_Course.domain.email.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TempMemberInfo implements Serializable {
    private String username;
    private String password;
    private String nickname;
    private String email;
    private String profileImageUrl;
    private String verificationCode;
}
