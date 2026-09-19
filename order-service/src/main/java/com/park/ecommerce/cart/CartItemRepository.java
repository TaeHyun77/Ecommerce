package com.park.ecommerce.cart;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    // 회원 조건을 함께 걸어 다른 회원의 항목에는 접근할 수 없도록 함
    Optional<CartItem> findByMemberIdAndProductId(Long memberId, Long productId);

    // 최근에 담은 항목이 위에 오도록 정렬 - 수량만 바꾼 항목이 위로 올라오지 않게 createdAt을 기준
    List<CartItem> findAllByMemberIdOrderByCreatedAtDesc(Long memberId);
}