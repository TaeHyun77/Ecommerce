package com.park.ecommerce.inbound.reception;

public enum InboundInterfaceStatus {
    PENDING, // 처리 대기 - 재시도 대기도 포함하며 next_retry_at으로 구분
    DONE, // 입고 예정 생성 완료
    FAILED // 재시도 소진 또는 재시도해도 해결되지 않는 오류
}
