package com.park.ecommerce.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// JWT를 직접 검증하지 않고, 게이트웨이가 이미 검증 후 심어준 신뢰 헤더를 그대로 사용한다.
// 게이트웨이를 거치지 않고 이 서비스에 직접 접근하면 헤더가 없어 인증되지 않는다 -
// 서비스가 게이트웨이를 통해서만 노출되는 것을 전제로 한다(인프라 레벨 과제).
public class GatewayHeaderAuthenticationFilter extends OncePerRequestFilter {
    private static final String MEMBER_ID_HEADER = "X-Member-Id";
    private static final String ROLE_HEADER = "X-Role";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain
    ) throws ServletException, IOException {
        String memberId = request.getHeader(MEMBER_ID_HEADER);
        String role = request.getHeader(ROLE_HEADER);

        if (StringUtils.hasText(memberId) && StringUtils.hasText(role)) {
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
            var authentication = new UsernamePasswordAuthenticationToken(Long.valueOf(memberId), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
