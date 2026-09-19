package com.park.ecommerce.security;

import com.park.ecommerce.member.domain.Member;
import com.park.ecommerce.member.domain.Provider;
import com.park.ecommerce.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private CustomOAuth2UserService customOAuth2UserService;

    @Test
    @DisplayName("활성 회원이 재로그인하면 상태를 유지한 채 프로필만 갱신한다")
    void updatesProfileWhenActiveMemberLogsInAgain() {
        Member member = member();
        given(memberRepository.findByProviderAndProviderId(Provider.KAKAO, "12345")).willReturn(Optional.of(member));

        Member result = customOAuth2UserService.saveOrUpdate(Provider.KAKAO, attributes("변경된 닉네임"));

        assertThat(result.isWithdrawn()).isFalse();
        assertThat(result.getNickname()).isEqualTo("변경된 닉네임");
    }

    @Test
    @DisplayName("탈퇴한 회원이 동일 소셜 계정으로 재로그인하면 재가입(재활성화)된다")
    void reactivatesWithdrawnMemberOnRelogin() {
        Member member = member();
        member.withdraw();
        given(memberRepository.findByProviderAndProviderId(Provider.KAKAO, "12345")).willReturn(Optional.of(member));

        Member result = customOAuth2UserService.saveOrUpdate(Provider.KAKAO, attributes("새 닉네임"));

        assertThat(result.isWithdrawn()).isFalse();
        assertThat(result.getNickname()).isEqualTo("새 닉네임");
    }

    private static Member member() {
        return Member.builder()
                .provider(Provider.KAKAO)
                .providerId("12345")
                .email("test@example.com")
                .nickname("테스트유저")
                .build();
    }

    private static OAuthAttributes attributes(String nickname) {
        return OAuthAttributes.of(Provider.KAKAO, kakaoAttributes(nickname));
    }

    private static java.util.Map<String, Object> kakaoAttributes(String nickname) {
        return java.util.Map.of(
                "id", 12345,
                "kakao_account", java.util.Map.of(
                        "email", "test@example.com",
                        "profile", java.util.Map.of(
                                "nickname", nickname,
                                "profile_image_url", "http://image.example.com/new.png"
                        )
                )
        );
    }
}
