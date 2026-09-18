package com.park.ecommerce.order;

import com.park.ecommerce.order.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {
    private final OrderRepository orderRepository;

    public List<OrderResponse> getOrdersByMemberId(Long memberId) {
        return orderRepository.findAllByMemberIdOrderByOrderedAtDesc(memberId).stream()
                .map(OrderResponse::from)
                .toList();
    }
}
