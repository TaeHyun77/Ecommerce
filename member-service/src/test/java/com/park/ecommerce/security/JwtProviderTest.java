package com.park.ecommerce.security;

import com.park.ecommerce.member.domain.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {
    private static final String SECRET = "test-jwt-secret-key-for-unit-test-should-be-long-enough";

    private final JwtProvider jwtProvider = new JwtProvider(SECRET, 1_000L * 60, 1_000L * 60 * 60);

    @Test
    @DisplayName("Access Token을 발급하면 회원 식별자와 권한을 그대로 복원할 수 있다")
    void createsAndParsesAccessToken() {
        String token = jwtProvider.createAccessToken(1L, Role.USER);

        assertThat(jwtProvider.isValid(token)).isTrue();
        assertThat(jwtProvider.getMemberId(token)).isEqualTo(1L);
        assertThat(jwtProvider.getRole(token)).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("Refresh Token에는 권한 정보를 담지 않는다")
    void refreshTokenHasNoRole() {
        String token = jwtProvider.createRefreshToken(1L);

        assertThat(jwtProvider.getMemberId(token)).isEqualTo(1L);
        assertThat(jwtProvider.getRole(token)).isNull();
    }

    @Test
    @DisplayName("만료된 토큰은 유효하지 않다")
    void expiredTokenIsInvalid() throws InterruptedException {
        JwtProvider shortLivedProvider = new JwtProvider(SECRET, 1L, 1L);
        String token = shortLivedProvider.createAccessToken(1L, Role.USER);

        Thread.sleep(10);

        assertThat(shortLivedProvider.isValid(token)).isFalse();
    }

    @Test
    @DisplayName("형식이 올바르지 않은 토큰은 유효하지 않다")
    void malformedTokenIsInvalid() {
        assertThat(jwtProvider.isValid("not-a-valid-jwt")).isFalse();
    }
}
