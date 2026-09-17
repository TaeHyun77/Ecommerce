package com.park.ecommerce.security;

import com.park.ecommerce.member.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtProvider {
    private static final String ROLE_CLAIM = "role";

    private final SecretKey key;
    private final long accessTokenExpirationMillis;
    private final long refreshTokenExpirationMillis;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpirationMillis,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpirationMillis
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMillis = accessTokenExpirationMillis;
        this.refreshTokenExpirationMillis = refreshTokenExpirationMillis;
    }

    public String createAccessToken(Long memberId, Role role) {
        return createToken(memberId, role, accessTokenExpirationMillis);
    }

    public String createRefreshToken(Long memberId) {
        return createToken(memberId, null, refreshTokenExpirationMillis);
    }

    public long getRefreshTokenExpirationMillis() {
        return refreshTokenExpirationMillis;
    }

    public Long getMemberId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    public Role getRole(String token) {
        String role = parseClaims(token).get(ROLE_CLAIM, String.class);
        return role == null ? null : Role.valueOf(role);
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private String createToken(Long memberId, Role role, long expirationMillis) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMillis);

        JwtBuilder builder = Jwts.builder()
                .subject(String.valueOf(memberId))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key);

        if (role != null) {
            builder.claim(ROLE_CLAIM, role.name());
        }

        return builder.compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
