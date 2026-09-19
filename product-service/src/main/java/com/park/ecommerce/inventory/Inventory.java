package com.park.ecommerce.inventory;

import com.park.ecommerce.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 상품별 재고 수량
// 입고 반영과 주문 차감이 상품 정보 수정과 같은 행을 두고 경합하지 않도록 Product와 분리했습니다.
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Inventory extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long productId; // 상품당 재고 1건 - 연관관계 없이 식별자만 저장

    @Column(nullable = false)
    private Integer quantity;

    @Builder
    private Inventory(Long productId, Integer quantity) {
        validateProductId(productId);
        validateQuantity(quantity);

        this.productId = productId;
        this.quantity = quantity;
    }

    public boolean isSoldOut() {
        return quantity == 0;
    }

    private static void validateProductId(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("상품은 필수입니다.");
        }
    }

    private static void validateQuantity(Integer quantity) {
        if (quantity == null || quantity < 0) {
            throw new IllegalArgumentException("재고 수량은 0개 이상이어야 합니다.");
        }
    }
}
