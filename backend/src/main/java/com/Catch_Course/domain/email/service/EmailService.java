package com.Catch_Course.domain.email.service;

import com.Catch_Course.domain.email.dto.TempMemberInfo;
import com.Catch_Course.global.exception.ServiceException;
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

    private static final String RESTORE_PREFIX = "restore ";

    // 인증 코드 생성
    public String createVerificationCode() {
        Random random = new Random();
        return String.valueOf(100000 + random.nextInt(900000));  // 6자리
    }

    // 임시 사용자 정보 Redis 저장
    public void saveTempMemberInfo(String email, String username,
                                   String nickname, String password,
                                   String profileImageUrl, String verificationCode) {

        TempMemberInfo info = new TempMemberInfo(username, password, nickname, email, profileImageUrl, verificationCode);
        redisTemplate.opsForValue().set(email, info, expires);
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

    // 인증 코드 검증 및 임시 회원 정보 반환
    public TempMemberInfo verifyCode(String email, String verificationCode) {
        TempMemberInfo tempMemberInfo = getMemberInfo(email);

        if (tempMemberInfo == null) {
            throw new ServiceException("401-4", "유효하지 않은 인증 요청입니다.");
        }

        if (!tempMemberInfo.getVerificationCode().equals(verificationCode)) {
            throw new ServiceException("401-5", "잘못된 인증 코드입니다.");
        }

        return tempMemberInfo;
    }

    // 임시 회원 정보 반환
    public TempMemberInfo getMemberInfo(String email) {
        Object info = redisTemplate.opsForValue().get(email);
        if (info != null) {
            return (TempMemberInfo) info;
        }
        return null;
    }

    // 임시 회원 정보 삭제
    public void deleteTempMemberInfo(String email) {
        redisTemplate.delete(email);
    }

    // Redis 에 이메일과 인증 코드 저장
    public void saveEmailAndVerificationCode(String email, String verificationCode) {
        String key = RESTORE_PREFIX + email;
        redisTemplate.opsForValue().set(key, verificationCode, expires);
    }

    public void restoreVerifyCode(String email, String verificationCode) {
        String key = RESTORE_PREFIX + email;
        String redisValue = (String) redisTemplate.opsForValue().get(key);

        if (redisValue == null) {
            throw new ServiceException("401-4", "유효하지 않은 인증 요청입니다.");
        }
        if (!redisValue.equals(verificationCode)) {
            throw new ServiceException("401-5", "잘못된 인증 코드입니다.");
        }
    }

    public void deleteRestoreData(String email) {
        redisTemplate.delete(RESTORE_PREFIX + email);
    }
}
