package com.park.ecommerce.product;

import com.park.ecommerce.exception.ProductErrorCode;
import com.park.ecommerce.exception.ProductException;
import com.park.ecommerce.inventory.Inventory;
import com.park.ecommerce.inventory.InventoryRepository;
import com.park.ecommerce.product.dto.ProductCreateRequest;
import com.park.ecommerce.product.dto.ProductDetailResponse;
import com.park.ecommerce.product.dto.ProductListResponse;
import com.park.ecommerce.product.dto.ProductPageResponse;
import com.park.ecommerce.product.dto.ProductResponse;
import com.park.ecommerce.product.dto.ProductSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    // 상품 마스터와 재고(0개)를 함께 생성
    // 재고는 입고 확정으로만 늘어나므로 등록 시점에는 항상 0개
    @Transactional
    public ProductResponse register(ProductCreateRequest request) {
        if (productRepository.existsByProductCode(request.productCode())) {
            throw new ProductException(ProductErrorCode.DUPLICATE_PRODUCT_CODE);
        }

        Product product = productRepository.save(request.toEntity());
        inventoryRepository.save(Inventory.builder()
                .productId(product.getId())
                .quantity(0)
                .build());

        return ProductResponse.from(product);
    }

    // 판매중 상품만 최신 등록순으로 노출 - 판매중지 상품은 숨기고, 품절은 soldOut으로 표시
    // 정렬은 클라이언트가 임의 필드를 지정하지 못하도록 고정
    public ProductPageResponse findOnSaleProducts(Long categoryId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Product> products = categoryId == null
                ? productRepository.findAllByStatus(ProductStatus.ON_SALE, pageRequest)
                : productRepository.findAllByStatusAndCategoryId(ProductStatus.ON_SALE, categoryId, pageRequest);

        // 페이지의 재고를 IN 조회 한 번으로 가져와 상품마다 재고를 조회하는 N+1을 피함
        List<Long> productIds = products.map(Product::getId).getContent();
        Map<Long, Inventory> inventories = inventoryRepository.findByProductIdIn(productIds).stream()
                .collect(Collectors.toMap(Inventory::getProductId, Function.identity()));

        return ProductPageResponse.from(products.map(product ->
                ProductListResponse.of(product, isSoldOut(inventories.get(product.getId())))));
    }

    // 판매중지 상품은 목록과 같은 기준으로 없는 상품처럼 404
    public ProductDetailResponse findOnSaleProduct(Long productId) {
        Product product = productRepository.findByIdAndStatus(productId, ProductStatus.ON_SALE)
                .orElseThrow(() -> new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND));
        Inventory inventory = inventoryRepository.findByProductId(productId).orElse(null);

        return ProductDetailResponse.of(product, isSoldOut(inventory));
    }

    // 재고 행은 상품 등록 시 함께 생성되지만, 없더라도 조회는 막지 않고 품절로 취급
    private static boolean isSoldOut(Inventory inventory) {
        return inventory == null || inventory.isSoldOut();
    }

    // 등록되지 않은 상품 식별자는 결과에서 빠진다 - 호출하는 쪽에서 누락 여부로 미등록을 판단
    public List<ProductSummaryResponse> findSummaries(Collection<Long> productIds) {
        Map<Long, Integer> quantities = inventoryRepository.findByProductIdIn(productIds).stream()
                .collect(Collectors.toMap(Inventory::getProductId, Inventory::getQuantity));

        return productRepository.findAllById(productIds).stream()
                // 재고 행은 상품 등록 시 함께 생성되지만, 없더라도 조회는 막지 않고 품절로 취급
                .map(product -> ProductSummaryResponse.of(product, quantities.getOrDefault(product.getId(), 0)))
                .toList();
    }

    // 등록되지 않은 상품코드는 결과에서 빠진다 - 호출하는 쪽에서 누락 여부로 미등록을 판단
    public Map<String, Long> findProductIdsByCodes(Collection<String> productCodes) {
        return productRepository.findAllByProductCodeIn(productCodes).stream()
                .collect(Collectors.toMap(Product::getProductCode, Product::getId));
    }
}
