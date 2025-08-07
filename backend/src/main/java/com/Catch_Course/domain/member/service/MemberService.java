package com.Catch_Course.domain.member.service;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.member.repository.MemberRepository;
import com.Catch_Course.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final AuthTokenService authTokenService;
    private final PasswordEncoder passwordEncoder;
    public Member join(String username, String password, String nickname, String email, String profileImageUrl) {
        String encodedPassword = passwordEncoder.encode(password);  // 패스워드 인코딩

        Member member = Member.builder()
                .username(username)
                .password(encodedPassword)
                .apiKey(username)
                .nickname(nickname)
                .email(email)
                .profileImageUrl(profileImageUrl)
                .isEmailVerified(true)
                .deleteFlag(false)
                .build();

        return memberRepository.save(member);
    }

    public long count() {
        return memberRepository.count();
    }

    public Optional<Member> findByUsername(String username) {
        return memberRepository.findByUsernameAndDeleteFlagFalse(username);
    }

    public Optional<Member> findByUsernameAll(String username) {
        return memberRepository.findByUsername(username);
    }

    public Optional<Member> findById(long id) {
        return memberRepository.findById(id);
    }

    public Optional<Member> findByApiKey(String apiKey) {
        return memberRepository.findByApiKey(apiKey);
    }

    // apiKey + accessToken
    public String getAuthToken(Member member) {

        String accessToken = authTokenService.createAccessToken(member);
        String apiKey = member.getApiKey();

        return apiKey + " " + accessToken;  // apiKey 와 accessToken 같이 반환
    }

    public Optional<Member> findMemberByAccessToken(String accessToken) {
        // 1. payload 가져옴
        Map<String, Object> payload = authTokenService.getPayload(accessToken);

        if (payload == null) return Optional.empty();

        // 2. payload 에서 id를 꺼냄
        Long id = (Long) payload.get("id");
        String username = (String) payload.get("username");

        return Optional.of(
                Member.builder()
                        .id(id)
                        .username(username)
                        .build()
        );
    }

    public String getAccessToken(Member member) {
        return authTokenService.createAccessToken(member);
    }

    public Optional<Member> findByEmail(String email) {
        return memberRepository.findByEmail(email);
    }

    // 인증 전 중복 체크
    public void checkVerification(String username, String email) {
        findByUsername(username).ifPresent(member -> {
            throw new ServiceException("400-1", "중복된 아이디입니다.");
        });

        Optional<Member> optionalMember = findByEmail(email);
        if (optionalMember.isPresent()) {

            // 계정 하드 삭제 전 가입 시도
            if(optionalMember.get().isDeleteFlag()){
                throw new ServiceException("400-3","계정을 백업할 수 있습니다.");
            }

            throw new ServiceException("400-2", "중복된 이메일입니다.");
        }
    }

    public void withdraw(Long memberId) {
        // 동시성 문제를 위해 id를 전달하여 DB 호출
        // todo: 이후 수강한 강의를 취소하는것까지 구현
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException("404-4","회원을 찾을 수 없습니다."));

        member.setDeleteFlag(true);
        memberRepository.save(member);
    }

    public Member restoreMember(Member member) {
        member.setDeleteFlag(false);
        return memberRepository.save(member);
    }

    public Member updateProfile(Long memberId, String nickname, String profileImageUrl) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException("404-4","회원을 찾을 수 없습니다."));

        member.setNickname(nickname);
        member.setProfileImageUrl(profileImageUrl);
        return memberRepository.save(member);
    }

    // 테스트용 하드 삭제
    public void deleteMember(Member member) {
        memberRepository.delete(member);
    }

    public Member updatePasswordAndEmail(Long memberId, String password, String email, String newPassword) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException("404-4","회원을 찾을 수 없습니다."));

        // 비밀번호 검증
        if(!passwordEncoder.matches(password, member.getPassword())){
            throw new ServiceException("403-3","비밀번호가 올바르지 않습니다.");
        }

        member.setPassword(passwordEncoder.encode(newPassword));
        member.setEmail(email);
        return memberRepository.save(member);
    }
}