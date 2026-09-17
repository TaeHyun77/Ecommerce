package com.park.ecommerce.member.domain;

// 지원하는 소셜 로그인 제공자
public enum Provider {
    KAKAO,
    GOOGLE;

    public static Provider from(String registrationId) {
        return Provider.valueOf(registrationId.toUpperCase());
    }
}
