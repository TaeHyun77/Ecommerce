package com.park.ecommerce.product;

import com.park.ecommerce.product.dto.ProductCreateRequest;
import com.park.ecommerce.product.dto.ProductResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// 공급사/MD용 상품 관리 API - 게이트웨이에서 /api/products/**는 인증 없이 열려 있어 경로를 분리했고, 아직 게이트웨이에 라우팅하지 않는다.
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class ProductAdminController {
    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // 상품 등록은 새 리소스를 만드는 요청이기에 200이 아니라 201 Created로 반환하도록 함
    public ProductResponse register(@Valid @RequestBody ProductCreateRequest request) {
        return productService.register(request);
    }
}