package com.Catch_Course.domain.member.service;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final AuthTokenService authTokenService;

    public Member join(String username, String password, String nickname) {
        Member member = Member.builder()
                .username(username)
                .password(password)
                .apiKey(username)
                .nickname(nickname)
                .build();

        return memberRepository.save(member);
    }

    public long count() {
        return memberRepository.count();
    }

    public Optional<Member> findByUsername(String username) {
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

}