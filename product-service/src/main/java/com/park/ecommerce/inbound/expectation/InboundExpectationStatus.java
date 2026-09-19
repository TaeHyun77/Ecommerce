package com.park.ecommerce.inbound.expectation;

public enum InboundExpectationStatus {
    EXPECTED, // 입고 예정 - 실물 도착/검수 대기
    COMPLETED // 입고 확정 완료 - 재고 반영됨
}
