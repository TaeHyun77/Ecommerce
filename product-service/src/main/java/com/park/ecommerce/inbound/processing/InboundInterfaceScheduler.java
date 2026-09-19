package com.park.ecommerce.inbound.processing;

import com.park.ecommerce.inbound.reception.InboundInterfaceRepository;
import com.park.ecommerce.inbound.reception.InboundInterfaceStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

// 단일 인스턴스를 가정 - 여러 대를 띄우면 같은 행을 동시에 처리할 수 있어 SELECT ... FOR UPDATE SKIP LOCKED가 필요
@Slf4j
@Component
@RequiredArgsConstructor
public class InboundInterfaceScheduler {
    private static final int BATCH_SIZE = 100;

    private final InboundInterfaceRepository inboundInterfaceRepository;
    private final InboundInterfaceProcessor inboundInterfaceProcessor;

    @Scheduled(fixedDelayString = "${inbound.interface.poll-delay}")
    public void processPendingInterfaces() {
        List<Long> targetIds = inboundInterfaceRepository.findProcessTargetIds(
                InboundInterfaceStatus.PENDING, LocalDateTime.now(), PageRequest.of(0, BATCH_SIZE)
        );

        for (Long interfaceId : targetIds) {
            try {
                inboundInterfaceProcessor.process(interfaceId);
            } catch (RuntimeException e) {
                // 한 건의 실패가 같은 회차의 나머지 건 처리를 막지 않도록 건 단위로 격리
                log.error("[입고 처리] 예상하지 못한 오류 interfaceId={}", interfaceId, e);
                inboundInterfaceProcessor.recordUnexpectedFailure(interfaceId, e.getMessage());
            }
        }
    }
}
