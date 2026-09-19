package com.park.ecommerce.inventory;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final InventoryRepository inventoryRepository;

    @Transactional
    public void increase(Long productId, int quantity) {
        int updated = inventoryRepository.increaseQuantity(productId, quantity);
        // 재고 행은 상품 등록 시 함께 생성되므로 없으면 데이터 불일치 - 호출한 트랜잭션 전체를 롤백
        if (updated == 0) {
            throw new IllegalStateException("재고 정보가 없는 상품입니다. productId=" + productId);
        }
    }
}
