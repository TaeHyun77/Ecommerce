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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status;

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
        this.status = MemberStatus.ACTIVE;
    }

    // 재로그인 시 소셜 제공자 측 프로필이 바뀌었을 수 있어 최신 값으로 동기화
    public void updateProfile(String nickname, String profileImageUrl) {
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
    }

    // 탈퇴한 회원의 재로그인은 재가입으로 간주해 프로필을 갱신하며 활성 상태로 되돌린다.
    public void reactivate(String nickname, String profileImageUrl) {
        this.status = MemberStatus.ACTIVE;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
    }

    // 개인정보 보호를 위해 닉네임/프로필 이미지를 익명화하고, 재가입에 필요한 provider/providerId/email은 유지
    public void withdraw() {
        this.status = MemberStatus.WITHDRAWN;
        this.nickname = "탈퇴한 회원";
        this.profileImageUrl = null;
    }

    public boolean isWithdrawn() {
        return this.status == MemberStatus.WITHDRAWN;
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
