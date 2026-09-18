package com.park.ecommerce.order;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders") // order는 MySQL 예약어라 테이블명으로 사용할 수 없음
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // member-service의 Member를 직접 참조하지 않고 식별자만 보관
    private Long memberId;

    private Integer totalAmount; // 주문 총액 (원)

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private LocalDateTime orderedAt;

    public Order(Long memberId, Integer totalAmount) {
        this.memberId = memberId;
        this.totalAmount = totalAmount;
        this.status = OrderStatus.ORDERED;
        this.orderedAt = LocalDateTime.now();
    }
}
