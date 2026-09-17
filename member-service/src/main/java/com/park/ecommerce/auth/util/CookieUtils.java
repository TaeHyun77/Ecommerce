package com.park.ecommerce.auth.util;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

public class CookieUtils {
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    private CookieUtils() {}

    public static void addRefreshTokenCookie(HttpServletResponse response, String refreshToken, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true) // JS에서 접근 불가하게 하여 XSS로 인한 탈취를 막음
                .secure(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(maxAge)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public static void expireRefreshTokenCookie(HttpServletResponse response) {
        addRefreshTokenCookie(response, "", Duration.ZERO);
    }
}
