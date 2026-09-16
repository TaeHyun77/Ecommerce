package com.park.ecommerce.product;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ProductTest {

    @Test
    @DisplayName("등록된 상품은 판매중 상태로 시작한다")
    void startsOnSale() {
        Product product = validProduct().build();

        assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = -1)
    @DisplayName("판매가가 없거나 음수이면 예외가 발생한다")
    void rejectsInvalidPrice(Integer price) {
        assertThatIllegalArgumentException().isThrownBy(() -> validProduct().price(price).build());
    }

    @Test
    @DisplayName("판매가 0원은 허용한다")
    void allowsZeroPrice() {
        assertThatCode(() -> validProduct().price(0).build()).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = -1)
    @DisplayName("재고 수량이 없거나 음수이면 예외가 발생한다")
    void rejectsInvalidStockQuantity(Integer stockQuantity) {
        assertThatIllegalArgumentException().isThrownBy(() -> validProduct().stockQuantity(stockQuantity).build());
    }

    @Test
    @DisplayName("재고 수량 0개는 허용한다")
    void allowsZeroStockQuantity() {
        assertThatCode(() -> validProduct().stockQuantity(0).build()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("카테고리가 없으면 예외가 발생한다")
    void rejectsNullCategoryId() {
        assertThatIllegalArgumentException().isThrownBy(() -> validProduct().categoryId(null).build());
    }

    @Test
    @DisplayName("재고가 0개이면 품절이다")
    void isSoldOutWhenStockIsZero() {
        assertThat(validProduct().stockQuantity(0).build().isSoldOut()).isTrue();
    }

    @Test
    @DisplayName("재고가 1개라도 있으면 품절이 아니다")
    void isNotSoldOutWhenStockRemains() {
        assertThat(validProduct().stockQuantity(1).build().isSoldOut()).isFalse();
    }

    private static Product.ProductBuilder validProduct() {
        return Product.builder()
                .productCode("SKU-0001")
                .name("유기농 우유 900ml")
                .storageType(StorageType.REFRIGERATED)
                .price(3_000)
                .stockQuantity(10)
                .categoryId(1L);
    }
}
