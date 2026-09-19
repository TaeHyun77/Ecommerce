package com.park.ecommerce.inbound.receipt;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

// WMS가 검수를 마치고 보내는 입고 확정
public record InboundReceiptRequest(
        @NotBlank(message = "입고 확정 번호는 필수입니다.")
        String receiptNo,

        @NotBlank(message = "입고 예정서 번호는 필수입니다.")
        String asnNo,

        @NotEmpty(message = "확정 품목은 1개 이상이어야 합니다.")
        List<@Valid Line> lines
) {
    public record Line(
            @NotBlank(message = "상품코드는 필수입니다.")
            String productCode,

            @NotNull(message = "양품 수량은 필수입니다.")
            @PositiveOrZero(message = "양품 수량은 0개 이상이어야 합니다.")
            Integer acceptedQuantity,

            @NotNull(message = "불량 수량은 필수입니다.")
            @PositiveOrZero(message = "불량 수량은 0개 이상이어야 합니다.")
            Integer rejectedQuantity
    ) {
    }

    public boolean hasDuplicateProduct() {
        return lines.stream().map(Line::productCode).distinct().count() != lines.size();
    }
}
