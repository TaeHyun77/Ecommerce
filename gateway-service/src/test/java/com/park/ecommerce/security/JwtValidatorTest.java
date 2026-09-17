package com.park.ecommerce.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtValidatorTest {
    private static final String SECRET = "test-jwt-secret-key-for-unit-test-should-be-long-enough";

    private final JwtValidator jwtValidator = new JwtValidator(SECRET);

    @Test
    @DisplayName("member-service가 발급한 것과 같은 시크릿으로 서명된 토큰은 유효하다")
    void validatesTokenSignedWithSameSecret() {
        String token = tokenWithRole("USER");

        assertThat(jwtValidator.isValid(token)).isTrue();
        assertThat(jwtValidator.getMemberId(token)).isEqualTo(1L);
        assertThat(jwtValidator.getRole(token)).isEqualTo("USER");
    }

    @Test
    @DisplayName("다른 시크릿으로 서명된 토큰은 유효하지 않다")
    void rejectsTokenSignedWithDifferentSecret() {
        JwtValidator otherValidator = new JwtValidator("different-jwt-secret-key-should-also-be-long-enough");

        assertThat(otherValidator.isValid(tokenWithRole("USER"))).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰은 유효하지 않다")
    void rejectsExpiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Date past = new Date(System.currentTimeMillis() - 1_000);
        String expiredToken = Jwts.builder()
                .subject("1")
                .claim("role", "USER")
                .issuedAt(new Date(past.getTime() - 1_000))
                .expiration(past)
                .signWith(key)
                .compact();

        assertThat(jwtValidator.isValid(expiredToken)).isFalse();
    }

    private String tokenWithRole(String role) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        return Jwts.builder()
                .subject("1")
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 60_000))
                .signWith(key)
                .compact();
    }
}
