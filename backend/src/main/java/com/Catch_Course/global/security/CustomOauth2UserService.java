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

        String oauthId = oauth2User.getName();     // 식별자
        String providerType = userRequest.getClientRegistration().getRegistrationId();      // SSO 타입
        String username = providerType + "-" + oauthId;

        SocialInfo info = extractUserInfo(providerType, oauthId, oauth2User.getAttributes());

        Optional<Member> optionalMember = memberService.findByUsername(username);

        // 이미 존재하는 유저라면 닉네임만 확인해서 업데이트
        if (optionalMember.isPresent()) {
            Member member = optionalMember.get();
            member.update(info.nickname);
            return new SecurityUser(member);
        }

        // 새로 가입하는 유저
        Member member = memberService.join(username, "", info.nickname, info.email, info.profile_image);
        return new SecurityUser(member);
    }

    private static class SocialInfo {
        private final String nickname;
        private final String profile_image;
        private final String email;

        public SocialInfo(String nickname, String profileImage, String email) {
            this.nickname = nickname;
            this.profile_image = profileImage;
            this.email = email;
        }
    }

    private SocialInfo extractUserInfo(String providerType, String oauthId, Map<String, Object> attributes) {
        if (providerType.equals("kakao")) {
            Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");

            String nickname = oauthId + properties.get("nickname").toString();
            String profile_image = properties.get("profile_image").toString();
            String email = nickname + "@kakao.com";
            return new SocialInfo(nickname, profile_image, email);
        } else {
            String nickname = oauthId + attributes.get("name").toString();
            String profile_image = attributes.get("picture").toString();
            String email = attributes.get("email").toString();

            return new SocialInfo(nickname, profile_image, email);
        }
    }


}
