package com.park.ecommerce.cart;

import com.park.ecommerce.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 회원이 장바구니에 담아둔 상품 한 건
// 상품명, 가격은 보관하지 않음
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "cart_item",
        // 같은 상품이 두 행으로 나뉘지 않도록 DB에서 보장 - 동시에 담기 요청이 들어와도 합산 대상이 하나로 유지됨
        uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "product_id"})
)
public class CartItem extends BaseTimeEntity {
    public static final int MAX_QUANTITY = 99; // 1회 구매 제한

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // member-service의 Member를 직접 참조하지 않고 식별자만 보관
    @Column(nullable = false)
    private Long memberId;

    // product-service의 Product를 직접 참조하지 않고 식별자만 보관
    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Builder
    private CartItem(Long memberId, Long productId, Integer quantity) {
        validateMemberId(memberId);
        validateProductId(productId);
        validateQuantity(quantity);

        this.memberId = memberId;
        this.productId = productId;
        this.quantity = quantity;
    }

    public void addQuantity(int amount) {
        validateQuantity(this.quantity + amount);
        this.quantity += amount;
    }

    public void changeQuantity(int quantity) {
        validateQuantity(quantity);
        this.quantity = quantity;
    }

    private static void validateMemberId(Long memberId) {
        if (memberId == null) {
            throw new IllegalArgumentException("회원은 필수입니다.");
        }
    }

    private static void validateProductId(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("상품은 필수입니다.");
        }
    }

    private static void validateQuantity(Integer quantity) {
        if (quantity == null || quantity < 1 || quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException("수량은 1개 이상 " + MAX_QUANTITY + "개 이하여야 합니다.");
        }
    }
}
