package com.park.ecommerce.inbound.receipt;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// WMS 연동을 대신하는 내부 API - 실제 WMS가 없어 검수 완료 요청을 이 API로 대신함
@RestController
@RequestMapping("/internal/inbound-receipts")
@RequiredArgsConstructor
public class InboundReceiptController {
    private final InboundReceiptService inboundReceiptService;

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT) // 최초 반영과 중복 재전송 모두 같은 응답 - WMS가 재전송해도 안전하도록
    public void receive(@Valid @RequestBody InboundReceiptRequest request) {
        inboundReceiptService.receive(request);
    }
}
