package com.park.ecommerce.product;

import com.park.ecommerce.exception.ProductErrorCode;
import com.park.ecommerce.exception.ProductException;
import com.park.ecommerce.inventory.Inventory;
import com.park.ecommerce.inventory.InventoryRepository;
import com.park.ecommerce.product.dto.ProductCreateRequest;
import com.park.ecommerce.product.dto.ProductDetailResponse;
import com.park.ecommerce.product.dto.ProductPageResponse;
import com.park.ecommerce.product.dto.ProductResponse;
import com.park.ecommerce.product.dto.ProductSummaryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @Test
    @DisplayName("상품 식별자 목록으로 상품 정보와 재고 수량을 함께 조회한다")
    void findsSummariesWithQuantity() {
        given(productRepository.findAllById(List.of(1L))).willReturn(List.of(product(1L)));
        given(inventoryRepository.findByProductIdIn(List.of(1L)))
                .willReturn(List.of(Inventory.builder().productId(1L).quantity(7).build()));

        List<ProductSummaryResponse> summaries = productService.findSummaries(List.of(1L));

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).productId()).isEqualTo(1L);
        assertThat(summaries.get(0).price()).isEqualTo(3_000);
        assertThat(summaries.get(0).availableQuantity()).isEqualTo(7);
    }

    @Test
    @DisplayName("등록되지 않은 상품 식별자는 결과에서 빠진다")
    void skipsUnknownProductId() {
        given(productRepository.findAllById(List.of(1L, 99L))).willReturn(List.of(product(1L)));
        given(inventoryRepository.findByProductIdIn(List.of(1L, 99L)))
                .willReturn(List.of(Inventory.builder().productId(1L).quantity(7).build()));

        List<ProductSummaryResponse> summaries = productService.findSummaries(List.of(1L, 99L));

        assertThat(summaries).extracting(ProductSummaryResponse::productId).containsExactly(1L);
    }

    @Test
    @DisplayName("재고 행을 찾지 못한 상품은 재고 0개로 조회된다")
    void treatsMissingInventoryAsZero() {
        given(productRepository.findAllById(List.of(1L))).willReturn(List.of(product(1L)));
        given(inventoryRepository.findByProductIdIn(List.of(1L))).willReturn(List.of());

        List<ProductSummaryResponse> summaries = productService.findSummaries(List.of(1L));

        assertThat(summaries.get(0).availableQuantity()).isZero();
    }

    @Test
    @DisplayName("상품 목록은 판매중 상품만 최신 등록순으로 조회하고 재고가 없으면 품절로 표시한다")
    void findsOnSaleProductsWithSoldOut() {
        PageRequest expected = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "id"));
        given(productRepository.findAllByStatus(ProductStatus.ON_SALE, expected))
                .willReturn(new PageImpl<>(List.of(product(2L), product(1L)), expected, 2));
        given(inventoryRepository.findByProductIdIn(List.of(2L, 1L)))
                .willReturn(List.of(inventory(2L, 0), inventory(1L, 5)));

        ProductPageResponse response = productService.findOnSaleProducts(null, 0, 20);

        assertThat(response.content())
                .extracting(item -> item.productId() + ":" + item.soldOut())
                .containsExactly("2:true", "1:false");
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    @DisplayName("카테고리를 지정하면 해당 카테고리의 판매중 상품만 조회한다")
    void findsOnSaleProductsByCategory() {
        given(productRepository.findAllByStatusAndCategoryId(eq(ProductStatus.ON_SALE), eq(3L), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        productService.findOnSaleProducts(3L, 0, 20);

        verify(productRepository, never()).findAllByStatus(any(), any());
    }

    @Test
    @DisplayName("재고 행이 없는 상품은 조회를 막지 않고 품절로 표시한다")
    void treatsMissingInventoryAsSoldOut() {
        given(productRepository.findByIdAndStatus(1L, ProductStatus.ON_SALE)).willReturn(Optional.of(product(1L)));
        given(inventoryRepository.findByProductId(1L)).willReturn(Optional.empty());

        ProductDetailResponse response = productService.findOnSaleProduct(1L);

        assertThat(response.soldOut()).isTrue();
        assertThat(response.description()).isEqualTo("1등급 원유로 만든 유기농 우유");
    }

    @Test
    @DisplayName("판매중이 아니거나 없는 상품의 상세를 조회하면 예외가 발생한다")
    void rejectsNotOnSaleProduct() {
        given(productRepository.findByIdAndStatus(1L, ProductStatus.ON_SALE)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findOnSaleProduct(1L))
                .isInstanceOf(ProductException.class)
                .extracting("errorCode")
                .isEqualTo(ProductErrorCode.PRODUCT_NOT_FOUND);
    }

    private static Inventory inventory(Long productId, int quantity) {
        return Inventory.builder().productId(productId).quantity(quantity).build();
    }

    private static Product product(Long id) {
        Product product = request("SKU-0001").toEntity();
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private static ProductCreateRequest request(String productCode) {
        return new ProductCreateRequest(
                productCode, "유기농 우유 900ml", "컬리팜", "1등급 원유로 만든 유기농 우유",
                StorageType.REFRIGERATED, 3_000, null, 1L
        );
    }
}
