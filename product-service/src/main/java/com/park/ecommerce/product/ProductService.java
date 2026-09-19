package com.park.ecommerce.product;

import com.park.ecommerce.exception.ProductErrorCode;
import com.park.ecommerce.exception.ProductException;
import com.park.ecommerce.inventory.Inventory;
import com.park.ecommerce.inventory.InventoryRepository;
import com.park.ecommerce.product.dto.ProductCreateRequest;
import com.park.ecommerce.product.dto.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
