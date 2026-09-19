package com.park.ecommerce.inbound.reception;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 입고 담당자용 운영 API - 상품 관리 API와 같이 게이트웨이에 라우팅하지 않음
@RestController
@RequestMapping("/api/admin/inbound-interfaces")
@RequiredArgsConstructor
public class InboundInterfaceAdminController {
    private final InboundReceptionService inboundReceptionService;

    // 실패 원인(fail_reason)을 확인해 상품 등록 또는 공급사 정정 요청 후 재처리하기 위한 조회 - 예: ?status=FAILED
    @GetMapping
    public List<InboundInterfaceResponse> findAll(@RequestParam InboundInterfaceStatus status) {
        return inboundReceptionService.findAllByStatus(status);
    }

    @PostMapping("/{asnNo}/retry")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retry(@PathVariable String asnNo) {
        inboundReceptionService.retry(asnNo);
    }
}
