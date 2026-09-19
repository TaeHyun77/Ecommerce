package com.park.ecommerce.security;

import com.park.ecommerce.member.domain.Member;
import com.park.ecommerce.member.domain.Provider;
import com.park.ecommerce.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Provider provider = Provider.from(registrationId);

        OAuthAttributes attributes = OAuthAttributes.of(provider, oAuth2User.getAttributes());
        Member member = saveOrUpdate(provider, attributes);

        return new CustomOAuth2User(member.getId(), member.getRole(), oAuth2User.getAttributes());
    }

    Member saveOrUpdate(Provider provider, OAuthAttributes attributes) {
        return memberRepository.findByProviderAndProviderId(provider, attributes.getProviderId())
                .map(member -> {
                    if (member.isWithdrawn()) {
                        member.reactivate(attributes.getNickname(), attributes.getProfileImageUrl());
                    } else {
                        member.updateProfile(attributes.getNickname(), attributes.getProfileImageUrl());
                    }
                    return member;
                })
                .orElseGet(() -> memberRepository.save(
                        Member.builder()
                                .provider(provider)
                                .providerId(attributes.getProviderId())
                                .email(attributes.getEmail())
                                .nickname(attributes.getNickname())
                                .profileImageUrl(attributes.getProfileImageUrl())
                                .build()
                ));
    }
}
