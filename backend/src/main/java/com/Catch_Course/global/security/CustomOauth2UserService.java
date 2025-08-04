package com.Catch_Course.global.security;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class CustomOauth2UserService extends DefaultOAuth2UserService {

    private final MemberService memberService;

    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        String providerType = userRequest.getClientRegistration().getRegistrationId();      // SSO 타입
        String oauthId = oauth2User.getName();     // 식별자
        String username = providerType + " " + oauthId;

        Map<String,Object> attributes = oauth2User.getAttributes();
        Map<String,Object> properties = (Map<String, Object>) attributes.get("properties");

        String nickname = properties.get("nickname").toString();
        String profile_image = properties.get("profile_image").toString();

        Optional<Member> optionalMember = memberService.findByUsername(username);

        // 이미 존재하는 유저라면 닉네임만 확인해서 업데이트
        if(optionalMember.isPresent()) {
            Member member = optionalMember.get();
            member.update(nickname);
            return new SecurityUser(member);
        }

        // 새로 가입하는 유저
        Member member = memberService.join(username, "",nickname);
        return new SecurityUser(member);
    }



}
