package com.park.ecommerce.auth;

import com.park.ecommerce.auth.dto.TokenResponse;
import com.park.ecommerce.exception.AuthErrorCode;
import com.park.ecommerce.exception.AuthException;
import com.park.ecommerce.member.domain.Member;
import com.park.ecommerce.member.domain.Provider;
import com.park.ecommerce.member.domain.Role;
import com.park.ecommerce.member.repository.MemberRepository;
import com.park.ecommerce.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    private static final String SECRET = "test-jwt-secret-key-for-unit-test-should-be-long-enough";

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private MemberRepository memberRepository;

    private JwtProvider jwtProvider;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(SECRET, 1_000L * 60, 1_000L * 60 * 60);
        authService = new AuthService(jwtProvider, refreshTokenRepository, memberRepository);
    }

    @Test
    @DisplayName("저장된 Refresh Token과 일치하면 새 토큰 쌍을 발급하고 Redis 값을 갱신한다")
    void reissuesTokensWhenRefreshTokenMatches() {
        Member member = member();
        String refreshToken = jwtProvider.createRefreshToken(1L);

        given(refreshTokenRepository.findByMemberId(1L)).willReturn(Optional.of(refreshToken));
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        TokenResponse response = authService.reissue(refreshToken);

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        verify(refreshTokenRepository).save(any(), any(), anyLong());
    }

    @Test
    @DisplayName("유효하지 않은 토큰이면 재발급을 거부한다")
    void rejectsInvalidToken() {
        assertThatThrownBy(() -> authService.reissue("invalid-token"))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getErrorCode())
                .isEqualTo(AuthErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("Redis에 저장된 Refresh Token이 없으면 재발급을 거부한다")
    void rejectsWhenRefreshTokenNotFound() {
        String refreshToken = jwtProvider.createRefreshToken(1L);
        given(refreshTokenRepository.findByMemberId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.reissue(refreshToken))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getErrorCode())
                .isEqualTo(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
    }

    @Test
    @DisplayName("요청 토큰과 저장된 토큰이 다르면 탈취로 간주해 즉시 무효화한다")
    void invalidatesOnRefreshTokenMismatch() {
        String requestToken = jwtProvider.createRefreshToken(1L);
        String savedToken = jwtProvider.createRefreshToken(1L) + "diff";
        given(refreshTokenRepository.findByMemberId(1L)).willReturn(Optional.of(savedToken));

        assertThatThrownBy(() -> authService.reissue(requestToken))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getErrorCode())
                .isEqualTo(AuthErrorCode.REFRESH_TOKEN_MISMATCH);

        verify(refreshTokenRepository).deleteByMemberId(1L);
    }

    @Test
    @DisplayName("로그아웃하면 저장된 Refresh Token을 삭제한다")
    void logoutDeletesRefreshToken() {
        authService.logout(1L);

        verify(refreshTokenRepository).deleteByMemberId(1L);
        verify(memberRepository, never()).findById(any());
    }

    private static Member member() {
        Member member = Member.builder()
                .provider(Provider.KAKAO)
                .providerId("12345")
                .email("test@example.com")
                .nickname("테스트유저")
                .build();
        ReflectionTestUtils.setField(member, "id", 1L); // JPA가 채우는 id를 Mock 시나리오에서 대신 채움
        return member;
    }
}
