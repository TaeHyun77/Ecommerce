package com.park.ecommerce.exception;

/**
 * order-service 자체의 장애 (5xx, 타임아웃, 커넥션 거부)를 나타내는 예외
 * 서킷 브레이커가 실패로 집계할 대상을 타입으로 한정하기 위해 4xx와 분리
 */
public class OrderServiceUnavailableException extends MemberException {
    public OrderServiceUnavailableException() {
        super(MemberErrorCode.ORDER_SERVICE_UNAVAILABLE);
    }
}
