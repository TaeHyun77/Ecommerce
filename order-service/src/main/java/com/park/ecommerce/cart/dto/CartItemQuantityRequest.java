package com.park.ecommerce.cart.dto;

import com.park.ecommerce.cart.CartItem;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// 수량 0은 허용하지 않습니다. - 삭제는 DELETE로만 처리해 같은 결과를 만드는 경로를 하나로 유지
public record CartItemQuantityRequest(
        @NotNull(message = "수량은 필수입니다.")
        @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
        @Max(value = CartItem.MAX_QUANTITY, message = "수량은 99개 이하여야 합니다.")
        Integer quantity
) {
}
