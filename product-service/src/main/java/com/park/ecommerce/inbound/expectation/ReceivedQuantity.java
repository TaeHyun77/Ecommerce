package com.park.ecommerce.inbound.expectation;

// 검수 결과 한 품목의 양품·불량 수량
public record ReceivedQuantity(int accepted, int rejected) {
    public static final ReceivedQuantity NOT_RECEIVED = new ReceivedQuantity(0, 0); // 확정에서 빠진 품목 - 미입고

    public int total() {
        return accepted + rejected;
    }
}
