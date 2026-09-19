package com.park.ecommerce.cart;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class CartItemTest {

    @Test
    @DisplayName("같은 상품을 다시 담으면 수량이 합산된다")
    void addsQuantity() {
        CartItem cartItem = validCartItem().quantity(2).build();

        cartItem.addQuantity(3);

        assertThat(cartItem.getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("합산 결과가 최대 수량을 넘으면 예외가 발생한다")
    void rejectsAddingOverMaxQuantity() {
        CartItem cartItem = validCartItem().quantity(CartItem.MAX_QUANTITY).build();

        assertThatIllegalArgumentException().isThrownBy(() -> cartItem.addQuantity(1));
    }

    @Test
    @DisplayName("수량을 변경하면 합산이 아니라 지정한 값으로 설정된다")
    void changesQuantity() {
        CartItem cartItem = validCartItem().quantity(2).build();

        cartItem.changeQuantity(5);

        assertThat(cartItem.getQuantity()).isEqualTo(5);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, 100})
    @DisplayName("수량이 없거나 1개 미만이거나 최대 수량을 넘으면 예외가 발생한다")
    void rejectsInvalidQuantity(Integer quantity) {
        assertThatIllegalArgumentException().isThrownBy(() -> validCartItem().quantity(quantity).build());
    }

    private static CartItem.CartItemBuilder validCartItem() {
        return CartItem.builder()
                .memberId(1L)
                .productId(10L)
                .quantity(1);
    }
}
