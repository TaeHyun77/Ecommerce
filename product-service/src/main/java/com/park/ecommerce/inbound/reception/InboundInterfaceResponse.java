package com.park.ecommerce.inbound.reception;

import java.time.LocalDateTime;

public record InboundInterfaceResponse(
        String asnNo,
        InboundInterfaceStatus status,
        Integer retryCount,
        String failReason, // 예: "미등록 상품: SKU-0005, SKU-0007"
        LocalDateTime receivedAt,
        LocalDateTime nextRetryAt
) {
    public static InboundInterfaceResponse from(InboundInterface inboundInterface) {
        return new InboundInterfaceResponse(
                inboundInterface.getAsnNo(),
                inboundInterface.getStatus(),
                inboundInterface.getRetryCount(),
                inboundInterface.getFailReason(),
                inboundInterface.getReceivedAt(),
                inboundInterface.getNextRetryAt()
        );
    }
}
