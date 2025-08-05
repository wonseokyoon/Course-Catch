package com.Catch_Course.domain.email.service;

import com.Catch_Course.domain.email.dto.TempMemberInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional
public class EmailService {

    private final JavaMailSender mailSender;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final Duration expires = Duration.ofMinutes(5);

    public String createVerificationCode() {
        Random random = new Random();
        return String.valueOf(100000 + random.nextInt(900000));  // 6자리
    }

    // 임시 사용자 정보 저장
    public void saveTempMemberInfo(String email, String username,
                                   String nickname, String password,
                                   String profileImageUrl, String verificationCode) {

        TempMemberInfo info = new TempMemberInfo(username, password, nickname, email, profileImageUrl, verificationCode);
        redisTemplate.opsForValue().set(email, info, expires);
    }

    public TempMemberInfo getMemberInfo(String email) {
        Object info = redisTemplate.opsForValue().get(email);
        if (info != null) {
            return (TempMemberInfo) info;
        }
        return null;
    }

    public void deleteTempMemberInfo(String email) {
        redisTemplate.delete(email);
    }

    // 메일 전송
    public void sendVerificationCode(String email, String verificationCode) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);
        message.setSubject("[Course Catch] 회원가입 이메일 인증 코드");
        message.setText("안녕하세요! Course Catch 회원가입을 위한 인증 코드입니다.\n\n" +
                "인증 코드: " + verificationCode + "\n\n" +
                "이 코드를 회원가입 화면에 입력해주세요. 감사합니다.");

        mailSender.send(message);
    }
}
