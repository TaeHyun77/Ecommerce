package com.park.ecommerce.member;

import com.park.ecommerce.order.MemberOrderService;
import com.park.ecommerce.order.dto.MemberOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {
    private final MemberOrderService memberOrderService;

    @GetMapping("/test")
    public String test() {
        log.info("테스트 요청 처리 -> 응답: member");
        return "member";
    }

    @GetMapping("/me/orders")
    public List<MemberOrderResponse> getMyOrders(@AuthenticationPrincipal Long memberId) {
        return memberOrderService.getMyOrders(memberId);
    }
}
