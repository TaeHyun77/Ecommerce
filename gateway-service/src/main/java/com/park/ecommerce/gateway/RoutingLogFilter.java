package com.park.ecommerce.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

// 요청이 어떤 라우트에 매칭되어 어느 주소로 전달됐는지와 응답 결과를 남기는 필터
@Slf4j
@Component
public class RoutingLogFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        URI requestUrl = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
        long startTime = System.currentTimeMillis();

        log.info("요청 전달: {} {} -> route={}, uri={}", request.getMethod(), request.getPath(), route.getId(), requestUrl);

        return chain.filter(exchange)
                .doFinally(signal -> log.info("응답 반환: {} {} <- {} ({}ms)",
                        request.getMethod(), request.getPath(), exchange.getResponse().getStatusCode(), System.currentTimeMillis() - startTime));
    }

    // 전달 주소(GATEWAY_REQUEST_URL_ATTR)는 RouteToRequestUrlFilter가 채우므로 그 직후에 실행해야 함
    @Override
    public int getOrder() {
        return RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER + 1;
    }
}
