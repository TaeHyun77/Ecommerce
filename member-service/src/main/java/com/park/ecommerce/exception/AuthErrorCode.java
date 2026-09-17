package com.park.ecommerce.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AuthErrorCode {
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "재발급 정보를 찾을 수 없습니다. 다시 로그인해주세요."),
    REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "토큰 정보가 일치하지 않습니다. 다시 로그인해주세요."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다.");

    private final HttpStatus status;
    private final String message;

    AuthErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
