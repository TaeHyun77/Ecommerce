package com.park.ecommerce.inbound.reception;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// 공급사용 API - 아직 게이트웨이에 라우팅하지 않고 product-service로 직접 호출
@RestController
@RequestMapping("/api/partner/inbound-expectations")
@RequiredArgsConstructor
public class InboundReceptionController {
    private final InboundReceptionService inboundReceptionService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED) // 접수만 하고 입고 예정 생성은 비동기로 처리하므로 201이 아닌 202
    public void receive(@Valid @RequestBody InboundExpectationRequest request) {
        inboundReceptionService.receive(request);
    }
}
