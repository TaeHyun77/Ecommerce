package com.park.ecommerce.member.domain;

import com.park.ecommerce.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "providerId"}))
public class Member extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;

    @Column(nullable = false)
    private String providerId; // 소셜 제공자가 내려주는 사용자 고유 식별자

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String nickname;

    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Builder
    private Member(Provider provider, String providerId, String email, String nickname, String profileImageUrl) {
        validateProvider(provider);
        validateProviderId(providerId);
        validateEmail(email);

        this.provider = provider;
        this.providerId = providerId;
        this.email = email;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.role = Role.USER; // 소셜 로그인 최초 가입은 일반 회원으로 고정
    }

    // 재로그인 시 소셜 제공자 측 프로필이 바뀌었을 수 있어 최신 값으로 동기화
    public void updateProfile(String nickname, String profileImageUrl) {
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
    }

    private static void validateProvider(Provider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("소셜 로그인 제공자는 필수입니다.");
        }
    }

    private static void validateProviderId(String providerId) {
        if (providerId == null || providerId.isBlank()) {
            throw new IllegalArgumentException("제공자 사용자 식별자는 필수입니다.");
        }
    }

    private static void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("이메일은 필수입니다.");
        }
    }
}
