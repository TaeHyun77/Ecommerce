package com.park.ecommerce.product;

import com.park.ecommerce.product.dto.ProductDetailResponse;
import com.park.ecommerce.product.dto.ProductPageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 고객용 상품 조회 API - 게이트웨이에서 /api/products/**는 인증 없이 열려 있음
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public ProductPageResponse getProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
            @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.") int size // 한 번에 과도한 조회를 막기 위한 상한
    ) {
        return productService.findOnSaleProducts(categoryId, page, size);
    }

    @GetMapping("/{productId}")
    public ProductDetailResponse getProduct(@PathVariable Long productId) {
        return productService.findOnSaleProduct(productId);
    }
}
