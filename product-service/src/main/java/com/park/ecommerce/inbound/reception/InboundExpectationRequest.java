package com.park.ecommerce.inbound.reception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.List;

// 공급사가 보내는 입고 예정서 - 인터페이스 테이블에 이 형태 그대로 JSON으로 저장
public record InboundExpectationRequest(
        @NotBlank(message = "입고 예정서 번호는 필수입니다.")
        String asnNo,

        @NotBlank(message = "공급사 코드는 필수입니다.")
        String supplierCode,

        @NotNull(message = "도착 예정일시는 필수입니다.")
        LocalDateTime expectedArrivalAt,

        @NotEmpty(message = "입고 품목은 1개 이상이어야 합니다.")
        List<@Valid Line> lines
) {
    public record Line(
            @NotBlank(message = "상품코드는 필수입니다.")
            String productCode,

            @NotNull(message = "예정 수량은 필수입니다.")
            @Positive(message = "예정 수량은 1개 이상이어야 합니다.")
            Integer quantity
    ) {
    }

    public List<String> productCodes() {
        return lines.stream().map(Line::productCode).toList();
    }

    public boolean hasDuplicateProduct() {
        return productCodes().stream().distinct().count() != lines.size();
    }
}
