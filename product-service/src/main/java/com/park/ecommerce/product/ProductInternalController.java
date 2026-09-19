package com.park.ecommerce.product;

import com.park.ecommerce.product.dto.ProductSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 게이트웨이가 외부로 노출하지 않는 서비스 간 내부 통신 전용 API - 인프라(게이트웨이 라우팅 설정)에서 별도로 차단해야 한다.
// 장바구니처럼 상품 여러 건이 필요한 쪽에서 건별로 호출하지 않도록 식별자 목록을 한 번에 받는다.
@RestController
@RequestMapping("/internal/products")
@RequiredArgsConstructor
public class ProductInternalController {
    private final ProductService productService;

    @GetMapping
    public List<ProductSummaryResponse> getProducts(@RequestParam List<Long> ids) {
        return productService.findSummaries(ids);
    }
}
