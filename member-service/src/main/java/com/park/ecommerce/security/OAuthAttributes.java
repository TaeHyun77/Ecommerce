package com.park.ecommerce.security;

import com.park.ecommerce.member.domain.Provider;
import lombok.Getter;

import java.util.Map;

// 제공자별로 다른 사용자 정보 응답 구조를 공통 형태로 정규화
@Getter
public class OAuthAttributes {
    private final String providerId;
    private final String email;
    private final String nickname;
    private final String profileImageUrl;

    private OAuthAttributes(String providerId, String email, String nickname, String profileImageUrl) {
        this.providerId = providerId;
        this.email = email;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
    }

    public static OAuthAttributes of(Provider provider, Map<String, Object> attributes) {
        return switch (provider) {
            case GOOGLE -> ofGoogle(attributes);
            case KAKAO -> ofKakao(attributes);
        };
    }

    private static OAuthAttributes ofGoogle(Map<String, Object> attributes) {
        return new OAuthAttributes(
                (String) attributes.get("sub"),
                (String) attributes.get("email"),
                (String) attributes.get("name"),
                (String) attributes.get("picture")
        );
    }

    @SuppressWarnings("unchecked")
    private static OAuthAttributes ofKakao(Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        return new OAuthAttributes(
                String.valueOf(attributes.get("id")),
                (String) kakaoAccount.get("email"),
                (String) profile.get("nickname"),
                (String) profile.get("profile_image_url")
        );
    }
}
