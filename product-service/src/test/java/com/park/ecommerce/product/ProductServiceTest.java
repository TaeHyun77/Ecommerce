package com.park.ecommerce.product;

import com.park.ecommerce.exception.ProductErrorCode;
import com.park.ecommerce.exception.ProductException;
import com.park.ecommerce.inventory.Inventory;
import com.park.ecommerce.inventory.InventoryRepository;
import com.park.ecommerce.product.dto.ProductCreateRequest;
import com.park.ecommerce.product.dto.ProductResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("상품을 등록하면 판매중 상태로 저장되고 재고 0개가 함께 생성된다")
    void registersProductWithEmptyInventory() {
        given(productRepository.existsByProductCode("SKU-0001")).willReturn(false);
        given(productRepository.save(any(Product.class))).willAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            ReflectionTestUtils.setField(product, "id", 1L); // IDENTITY 전략으로 저장 시 부여되는 식별자를 흉내
            return product;
        });

        ProductResponse response = productService.register(request("SKU-0001"));

        ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(inventoryCaptor.capture());
        assertThat(inventoryCaptor.getValue().getProductId()).isEqualTo(1L);
        assertThat(inventoryCaptor.getValue().getQuantity()).isZero();

        assertThat(response.productId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(ProductStatus.ON_SALE);
    }

    @Test
    @DisplayName("이미 등록된 상품코드면 예외가 발생하고 아무것도 저장하지 않는다")
    void rejectsDuplicateProductCode() {
        given(productRepository.existsByProductCode("SKU-0001")).willReturn(true);

        assertThatThrownBy(() -> productService.register(request("SKU-0001")))
                .isInstanceOf(ProductException.class)
                .extracting("errorCode")
                .isEqualTo(ProductErrorCode.DUPLICATE_PRODUCT_CODE);

        verify(productRepository, never()).save(any());
        verify(inventoryRepository, never()).save(any());
    }

    private static ProductCreateRequest request(String productCode) {
        return new ProductCreateRequest(
                productCode, "유기농 우유 900ml", "컬리팜", "1등급 원유로 만든 유기농 우유",
                StorageType.REFRIGERATED, 3_000, null, 1L
        );
    }
}
