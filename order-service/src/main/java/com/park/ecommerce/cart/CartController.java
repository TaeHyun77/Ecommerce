package com.park.ecommerce.cart;

import com.park.ecommerce.cart.dto.CartItemAddRequest;
import com.park.ecommerce.cart.dto.CartItemQuantityRequest;
import com.park.ecommerce.cart.dto.CartItemResponse;
import com.park.ecommerce.cart.dto.CartResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// 회원은 게이트웨이가 JWT를 검증한 뒤 심어준 헤더로 식별 - 요청 파라미터로 받으면 남의 장바구니에 접근할 수 있음
@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {
    private static final String MEMBER_ID_HEADER = "X-Member-Id";
    private final CartService cartService;

    // 장바구니 목록 조회
    @GetMapping
    public CartResponse getCart(@RequestHeader(MEMBER_ID_HEADER) Long memberId) {
        return cartService.getCart(memberId);
    }

    // 장바구니에 상품 추가
    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartItemResponse addItem(
            @RequestHeader(MEMBER_ID_HEADER) Long memberId,
            @Valid @RequestBody CartItemAddRequest request
    ) {
        return cartService.addItem(memberId, request.productId(), request.quantity());
    }

    // 상품 수량 조절
    @PatchMapping("/items/{productId}")
    public CartItemResponse changeQuantity(
            @RequestHeader(MEMBER_ID_HEADER) Long memberId,
            @PathVariable Long productId,
            @Valid @RequestBody CartItemQuantityRequest request
    ) {
        return cartService.changeQuantity(memberId, productId, request.quantity());
    }

    // 장바구니에서 상품 삭제
    @DeleteMapping("/items/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 응답 본문이 없는 삭제는 204로 반환
    public void removeItem(
            @RequestHeader(MEMBER_ID_HEADER) Long memberId,
            @PathVariable Long productId
    ) {
        cartService.removeItem(memberId, productId);
    }
}
