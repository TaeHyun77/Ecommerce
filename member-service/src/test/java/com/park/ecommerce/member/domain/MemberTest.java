package com.park.ecommerce.member.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class MemberTest {

    @Test
    @DisplayName("소셜 로그인으로 가입한 회원은 일반 회원(USER) 권한으로 시작한다")
    void startsAsUserRole() {
        Member member = validMember().build();

        assertThat(member.getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("제공자가 없으면 예외가 발생한다")
    void rejectsNullProvider() {
        assertThatIllegalArgumentException().isThrownBy(() -> validMember().provider(null).build());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("제공자 사용자 식별자가 없거나 공백이면 예외가 발생한다")
    void rejectsInvalidProviderId(String providerId) {
        assertThatIllegalArgumentException().isThrownBy(() -> validMember().providerId(providerId).build());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("이메일이 없거나 공백이면 예외가 발생한다")
    void rejectsInvalidEmail(String email) {
        assertThatIllegalArgumentException().isThrownBy(() -> validMember().email(email).build());
    }

    @Test
    @DisplayName("프로필을 갱신하면 닉네임과 프로필 이미지가 최신 값으로 바뀐다")
    void updatesProfile() {
        Member member = validMember().build();

        member.updateProfile("새 닉네임", "https://example.com/new.png");

        assertThat(member.getNickname()).isEqualTo("새 닉네임");
        assertThat(member.getProfileImageUrl()).isEqualTo("https://example.com/new.png");
    }

    private static Member.MemberBuilder validMember() {
        return Member.builder()
                .provider(Provider.KAKAO)
                .providerId("12345")
                .email("test@example.com")
                .nickname("테스트유저")
                .profileImageUrl("https://example.com/profile.png");
    }
}
