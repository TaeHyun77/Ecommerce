package com.park.ecommerce.cart;

import com.park.ecommerce.cart.dto.CartProductResponse;
import com.park.ecommerce.exception.ProductServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductApiClient {
    private final RestClient productServiceRestClient;

    /** 등록되지 않은 상품 식별자는 응답에서 빠지므로, 호출하는 쪽에서 누락 여부로 미등록을 판단 - 상품 정보 조회
     * Circuit이 OPEN이면 실제 호출 없이 CallNotPermittedException 발생
     * → GlobalExceptionHandler에서 HTTP 503으로 변환
     */
    @CircuitBreaker(name = "productService")
    public List<CartProductResponse> findProducts(Collection<Long> productIds) {
        try {
            return productServiceRestClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/internal/products")
                            .queryParam("ids", productIds)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<CartProductResponse>>() {});
        } catch (ResourceAccessException e) {
            // 타임아웃, 커넥션 거부 등 HTTP 상태 코드 이전 단계의 통신 실패 - defaultStatusHandler로는 잡히지 않음
            throw new ProductServiceUnavailableException();
        }
    }
}
