package com.park.ecommerce.product.dto;

import org.springframework.data.domain.Page;

import java.util.List;

// Page를 그대로 반환하면 Spring Data 내부 구조가 응답 형식이 되어 버전에 따라 바뀔 수 있어 필요한 값만 담음
public record ProductPageResponse(
        List<ProductListResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public static ProductPageResponse from(Page<ProductListResponse> page) {
        return new ProductPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
