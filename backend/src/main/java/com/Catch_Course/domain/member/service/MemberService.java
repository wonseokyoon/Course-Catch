package com.Catch_Course.domain.member.service;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.member.repository.MemberRepository;
import com.Catch_Course.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
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
    private final RedisTemplate<String,Object> redisTemplate;

    @Value("${custom.jwt.refresh-expire-seconds}")
    private int refreshExpireSeconds;

    private static final String REFRESH_PREFIX = "refresh: ";

    public Member join(String username, String password, String nickname, String email, String profileImageUrl) {
        String encodedPassword = passwordEncoder.encode(password);  // 패스워드 인코딩

        Member member = Member.builder()
                .username(username)
                .password(encodedPassword)
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

    public Optional<Member> findById(long memberId) {
        return memberRepository.findById(memberId);
    }

    // accessToken + refreshToken
    public String getAuthToken(Member member) {

        String accessToken = authTokenService.createAccessToken(member);
        String refreshToken = authTokenService.createRefreshToken(member);

        return accessToken + " " + refreshToken;  // accessToken , refreshToken
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

    public Optional<Member> findMemberByRefreshToken(String refreshToken) {
        // 1. payload 가져옴
        Map<String, Object> payload = authTokenService.getPayload(refreshToken);

        if (payload == null) return Optional.empty();

        // 2. payload 에서 id를 꺼냄
        Long id = (Long) payload.get("id");
        String username = (String) payload.get("username");

        String storedRefreshToken = (String) redisTemplate.opsForValue().get(REFRESH_PREFIX + username);

        if(!refreshToken.equals(storedRefreshToken)) {
            return Optional.empty();
        }

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

    public String getRefreshToken(Member member) {
        return authTokenService.createRefreshToken(member);
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
            if (optionalMember.get().isDeleteFlag()) {
                throw new ServiceException("400-3", "계정을 백업할 수 있습니다.");
            }

            throw new ServiceException("400-2", "중복된 이메일입니다.");
        }
    }

    public void withdraw(Long memberId) {
        // 동시성 문제를 위해 id를 전달하여 DB 호출
        // todo: 이후 수강한 강의를 취소하는것까지 구현
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException("404-4", "회원을 찾을 수 없습니다."));

        member.setDeleteFlag(true);
        memberRepository.save(member);
    }

    public Member restoreMember(Member member) {
        member.setDeleteFlag(false);
        return memberRepository.save(member);
    }
}