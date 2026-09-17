package com.park.ecommerce.security;

import com.park.ecommerce.auth.RefreshTokenRepository;
import com.park.ecommerce.auth.util.CookieUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication
    ) throws IOException {
        CustomOAuth2User principal = (CustomOAuth2User) authentication.getPrincipal();

        String accessToken = jwtProvider.createAccessToken(principal.getMemberId(), principal.getRole());
        String refreshToken = jwtProvider.createRefreshToken(principal.getMemberId());

        refreshTokenRepository.save(principal.getMemberId(), refreshToken, jwtProvider.getRefreshTokenExpirationMillis());
        CookieUtils.addRefreshTokenCookie(
                response, refreshToken, Duration.ofMillis(jwtProvider.getRefreshTokenExpirationMillis())
        );

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("accessToken", accessToken)
                .build()
                .toUriString();

        response.sendRedirect(targetUrl);
    }
}
