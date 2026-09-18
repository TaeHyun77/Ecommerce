package com.park.ecommerce.order;

import com.park.ecommerce.exception.MemberErrorCode;
import com.park.ecommerce.exception.MemberException;
import com.park.ecommerce.order.dto.MemberOrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderApiClient {
    private final RestClient orderServiceRestClient;

    public List<MemberOrderResponse> findOrdersByMemberId(Long memberId) {
        try {
            return orderServiceRestClient.get()
                    .uri("/internal/orders?memberId={memberId}", memberId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<MemberOrderResponse>>() {});
        } catch (ResourceAccessException e) {
            // 타임아웃, 커넥션 거부 등 HTTP 상태 코드 이전 단계의 통신 실패 - defaultStatusHandler로는 잡히지 않음
            throw new MemberException(MemberErrorCode.ORDER_SERVICE_UNAVAILABLE);
        }
    }
}
