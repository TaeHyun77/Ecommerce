package com.park.ecommerce.auth;

import com.park.ecommerce.auth.dto.TokenResponse;
import com.park.ecommerce.auth.util.CookieUtils;
import com.park.ecommerce.security.JwtProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final JwtProvider jwtProvider;

    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(
            @CookieValue(value = CookieUtils.REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        TokenResponse tokenResponse = authService.reissue(refreshToken);
        CookieUtils.addRefreshTokenCookie(
                response, tokenResponse.getRefreshToken(), Duration.ofMillis(jwtProvider.getRefreshTokenExpirationMillis())
        );
        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication, HttpServletResponse response) {
        Long memberId = (Long) authentication.getPrincipal();
        authService.logout(memberId);
        CookieUtils.expireRefreshTokenCookie(response);
        return ResponseEntity.noContent().build();
    }
}
