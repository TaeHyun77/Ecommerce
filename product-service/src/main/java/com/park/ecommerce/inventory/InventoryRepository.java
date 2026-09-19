package com.park.ecommerce.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    // 조회 후 더해서 저장하면 주문 차감 등 동시 변경이 덮어써질 수 있어 DB에서 원자적으로 증가
    // 벌크 연산은 감사(Auditing)를 거치지 않으므로 updatedAt을 직접 갱신
    @Modifying
    @Query("update Inventory i set i.quantity = i.quantity + :quantity, i.updatedAt = current_timestamp where i.productId = :productId")
    int increaseQuantity(Long productId, int quantity);
}
