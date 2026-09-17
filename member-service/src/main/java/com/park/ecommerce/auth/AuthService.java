package com.park.ecommerce.auth;

import com.park.ecommerce.auth.dto.TokenResponse;
import com.park.ecommerce.exception.AuthErrorCode;
import com.park.ecommerce.exception.AuthException;
import com.park.ecommerce.member.domain.Member;
import com.park.ecommerce.member.repository.MemberRepository;
import com.park.ecommerce.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public TokenResponse reissue(String refreshToken) {
        if (refreshToken == null || !jwtProvider.isValid(refreshToken)) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }

        Long memberId = jwtProvider.getMemberId(refreshToken);

        String savedRefreshToken = refreshTokenRepository.findByMemberId(memberId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND));

        if (!savedRefreshToken.equals(refreshToken)) {
            // 저장된 토큰과 다르면 탈취되었을 가능성이 있으므로 즉시 무효화해 강제 재로그인을 유도한다.
            refreshTokenRepository.deleteByMemberId(memberId);
            throw new AuthException(AuthErrorCode.REFRESH_TOKEN_MISMATCH);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.MEMBER_NOT_FOUND));

        String newAccessToken = jwtProvider.createAccessToken(member.getId(), member.getRole());
        String newRefreshToken = jwtProvider.createRefreshToken(member.getId());
        refreshTokenRepository.save(member.getId(), newRefreshToken, jwtProvider.getRefreshTokenExpirationMillis());

        return TokenResponse.of(newAccessToken, newRefreshToken);
    }

    public void logout(Long memberId) {
        refreshTokenRepository.deleteByMemberId(memberId);
    }
}
