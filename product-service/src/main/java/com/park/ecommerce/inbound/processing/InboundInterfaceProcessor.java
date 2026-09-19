package com.park.ecommerce.inbound.processing;

import com.park.ecommerce.inbound.expectation.InboundExpectation;
import com.park.ecommerce.inbound.expectation.InboundExpectationRepository;
import com.park.ecommerce.inbound.reception.InboundExpectationRequest;
import com.park.ecommerce.inbound.reception.InboundInterface;
import com.park.ecommerce.inbound.reception.InboundInterfaceRepository;
import com.park.ecommerce.product.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// 인터페이스 1건을 입고 예정으로 변환
// 스케줄러와 다른 빈으로 분리 - 같은 클래스 내부 호출이면 건별 트랜잭션이 적용되지 않음
@Slf4j
@Component
public class InboundInterfaceProcessor {
    private final InboundInterfaceRepository inboundInterfaceRepository;
    private final InboundExpectationRepository inboundExpectationRepository;
    private final ProductService productService;
    private final JsonMapper jsonMapper;
    private final Duration retryInterval;
    private final int maxRetry;

    public InboundInterfaceProcessor(
            InboundInterfaceRepository inboundInterfaceRepository,
            InboundExpectationRepository inboundExpectationRepository,
            ProductService productService,
            JsonMapper jsonMapper,
            @Value("${inbound.interface.retry-interval}") Duration retryInterval,
            @Value("${inbound.interface.max-retry}") int maxRetry
    ) {
        this.inboundInterfaceRepository = inboundInterfaceRepository;
        this.inboundExpectationRepository = inboundExpectationRepository;
        this.productService = productService;
        this.jsonMapper = jsonMapper;
        this.retryInterval = retryInterval;
        this.maxRetry = maxRetry;
    }

    @Transactional
    public void process(Long interfaceId) {
        InboundInterface inboundInterface = inboundInterfaceRepository.findById(interfaceId).orElseThrow();
        if (!inboundInterface.isPending()) {
            return;
        }

        InboundExpectationRequest request;
        try {
            request = jsonMapper.readValue(inboundInterface.getPayload(), InboundExpectationRequest.class);
        } catch (JacksonException e) {
            // 수신 시 검증을 통과한 원본이라 정상이면 발생하지 않음 - 다시 시도해도 결과가 같으므로 즉시 실패 처리
            inboundInterface.fail("원본 역직렬화 실패: " + e.getOriginalMessage());
            log.error("[입고 처리] 원본 역직렬화 실패 asnNo={}", inboundInterface.getAsnNo(), e);
            return;
        }

        Map<String, Long> productIds = productService.findProductIdsByCodes(request.productCodes());
        List<String> missingProductCodes = request.productCodes().stream()
                .filter(productCode -> !productIds.containsKey(productCode))
                .toList();

        // 한 품목이라도 미등록이면 예정서 전체를 대기 - 예정서 1건이 입고 예정 1건으로만 만들어지도록
        if (!missingProductCodes.isEmpty()) {
            retryLater(inboundInterface, "미등록 상품: " + String.join(", ", missingProductCodes));
            return;
        }

        InboundExpectation expectation = InboundExpectation.builder()
                .asnNo(request.asnNo())
                .supplierCode(request.supplierCode())
                .expectedArrivalAt(request.expectedArrivalAt())
                .build();
        request.lines().forEach(line ->
                expectation.addLine(productIds.get(line.productCode()), line.productCode(), line.quantity()));

        inboundExpectationRepository.save(expectation);
        inboundInterface.markDone();
        log.info("[입고 처리] 입고 예정 생성 asnNo={}, 재시도 횟수={}", request.asnNo(), inboundInterface.getRetryCount());
    }

    // process()가 예상하지 못한 예외로 롤백된 경우 호출 - 재시도 횟수를 남기지 않으면 같은 건이 매 회차 무한 반복됨
    @Transactional
    public void recordUnexpectedFailure(Long interfaceId, String reason) {
        InboundInterface inboundInterface = inboundInterfaceRepository.findById(interfaceId).orElseThrow();
        retryLater(inboundInterface, "처리 중 오류: " + reason);
    }

    private void retryLater(InboundInterface inboundInterface, String reason) {
        inboundInterface.retryLater(reason, LocalDateTime.now(), retryInterval, maxRetry);

        if (inboundInterface.isPending()) {
            log.warn("[입고 처리] 재시도 예약 asnNo={}, 사유={}, 재시도 {}/{}, 다음 시도={}",
                    inboundInterface.getAsnNo(), reason, inboundInterface.getRetryCount(), maxRetry, inboundInterface.getNextRetryAt());
            return;
        }
        log.error("[입고 처리] 재시도 소진으로 실패 처리 asnNo={}, 사유={}", inboundInterface.getAsnNo(), reason);
    }
}
