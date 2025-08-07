package com.Catch_Course.domain.member.service;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.member.repository.MemberRepository;
import com.Catch_Course.global.exception.ServiceException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ProfileService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public Member updateProfile(Long memberId, String nickname, String newPassword, String email, String profileImageUrl,HttpSession session) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException("404-4", "회원을 찾을 수 없습니다."));

        member.setNickname(nickname);
        member.setPassword(passwordEncoder.encode(newPassword));
        member.setEmail(email);
        member.setProfileImageUrl(profileImageUrl);

        session.removeAttribute("passwordVerified");    // 인증 만료
        return memberRepository.save(member);
    }

    public void checkPassword(Long memberId, String password, HttpSession session) {

        session.setAttribute("passwordVerified", false);    // 세션을 일회성으로 사용하기 위해 설정

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException("404-4", "회원을 찾을 수 없습니다."));

        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new ServiceException("403-3", "비밀번호가 올바르지 않습니다.");
        }

        session.setAttribute("passwordVerified", true);
        session.setMaxInactiveInterval(300);
    }
}