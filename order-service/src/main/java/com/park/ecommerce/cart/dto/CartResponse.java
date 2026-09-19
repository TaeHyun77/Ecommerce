package com.park.ecommerce.cart.dto;

import java.util.List;

public record CartResponse(
        List<CartItemResponse> items,
        Integer totalAmount // 주문 가능한 항목의 금액 합계 (원)
) {
    public static CartResponse from(List<CartItemResponse> items) {
        int totalAmount = items.stream()
                .filter(CartItemResponse::orderable)
                .mapToInt(CartItemResponse::lineAmount)
                .sum();
        return new CartResponse(items, totalAmount);
    }
}
