package com.park.ecommerce.inventory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class InventoryTest {

    @Test
    @DisplayName("상품이 없으면 예외가 발생한다")
    void rejectsNullProductId() {
        assertThatIllegalArgumentException().isThrownBy(() -> validInventory().productId(null).build());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = -1)
    @DisplayName("재고 수량이 없거나 음수이면 예외가 발생한다")
    void rejectsInvalidQuantity(Integer quantity) {
        assertThatIllegalArgumentException().isThrownBy(() -> validInventory().quantity(quantity).build());
    }

    @Test
    @DisplayName("재고 수량 0개는 허용한다")
    void allowsZeroQuantity() {
        assertThatCode(() -> validInventory().quantity(0).build()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("재고가 0개이면 품절이다")
    void isSoldOutWhenQuantityIsZero() {
        assertThat(validInventory().quantity(0).build().isSoldOut()).isTrue();
    }

    @Test
    @DisplayName("재고가 1개라도 있으면 품절이 아니다")
    void isNotSoldOutWhenQuantityRemains() {
        assertThat(validInventory().quantity(1).build().isSoldOut()).isFalse();
    }

    private static Inventory.InventoryBuilder validInventory() {
        return Inventory.builder()
                .productId(1L)
                .quantity(10);
    }
}
