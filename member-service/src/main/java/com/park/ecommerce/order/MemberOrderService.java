package com.park.ecommerce.order;

import com.park.ecommerce.order.dto.MemberOrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberOrderService {
    private final OrderApiClient orderApiClient;

    public List<MemberOrderResponse> getMyOrders(Long memberId) {
        return orderApiClient.findOrdersByMemberId(memberId);
    }
}
