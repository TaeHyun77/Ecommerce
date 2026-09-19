package com.park.ecommerce.inbound.processing;

import com.park.ecommerce.inbound.expectation.InboundExpectation;
import com.park.ecommerce.inbound.expectation.InboundExpectationLine;
import com.park.ecommerce.inbound.expectation.InboundExpectationRepository;
import com.park.ecommerce.inbound.expectation.InboundExpectationStatus;
import com.park.ecommerce.inbound.reception.InboundExpectationRequest;
import com.park.ecommerce.inbound.reception.InboundInterface;
import com.park.ecommerce.inbound.reception.InboundInterfaceRepository;
import com.park.ecommerce.inbound.reception.InboundInterfaceStatus;
import com.park.ecommerce.product.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InboundInterfaceProcessorTest {
    private static final int MAX_RETRY = 3;

    @Mock
    private InboundInterfaceRepository inboundInterfaceRepository;

    @Mock
    private InboundExpectationRepository inboundExpectationRepository;

    @Mock
    private ProductService productService;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private InboundInterfaceProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new InboundInterfaceProcessor(
                inboundInterfaceRepository, inboundExpectationRepository, productService,
                jsonMapper, Duration.ofMinutes(1), MAX_RETRY
        );
    }

    @Test
    @DisplayName("모든 상품이 등록되어 있으면 입고 예정을 만들고 인터페이스를 완료 처리한다")
    void createsExpectation() {
        InboundInterface inboundInterface = pendingInterface();
        given(inboundInterfaceRepository.findById(1L)).willReturn(Optional.of(inboundInterface));
        given(productService.findProductIdsByCodes(List.of("SKU-0001", "SKU-0003")))
                .willReturn(Map.of("SKU-0001", 1L, "SKU-0003", 3L));

        processor.process(1L);

        ArgumentCaptor<InboundExpectation> captor = ArgumentCaptor.forClass(InboundExpectation.class);
        verify(inboundExpectationRepository).save(captor.capture());
        InboundExpectation expectation = captor.getValue();
        assertThat(expectation.getStatus()).isEqualTo(InboundExpectationStatus.EXPECTED);
        assertThat(expectation.getLines())
                .extracting(InboundExpectationLine::getProductId, InboundExpectationLine::getExpectedQuantity)
                .containsExactly(tuple(1L, 200), tuple(3L, 50));
        assertThat(inboundInterface.getStatus()).isEqualTo(InboundInterfaceStatus.DONE);
    }

    @Test
    @DisplayName("미등록 상품이 하나라도 있으면 입고 예정을 만들지 않고 재시도로 넘긴다")
    void retriesWhenProductMissing() {
        InboundInterface inboundInterface = pendingInterface();
        given(inboundInterfaceRepository.findById(1L)).willReturn(Optional.of(inboundInterface));
        given(productService.findProductIdsByCodes(any())).willReturn(Map.of("SKU-0001", 1L));

        processor.process(1L);

        verify(inboundExpectationRepository, never()).save(any());
        assertThat(inboundInterface.getStatus()).isEqualTo(InboundInterfaceStatus.PENDING);
        assertThat(inboundInterface.getRetryCount()).isEqualTo(1);
        assertThat(inboundInterface.getFailReason()).isEqualTo("미등록 상품: SKU-0003");
    }

    @Test
    @DisplayName("원본을 역직렬화할 수 없으면 재시도하지 않고 바로 실패 처리한다")
    void failsWhenPayloadBroken() {
        InboundInterface inboundInterface = new InboundInterface("ASN-0001", "{broken", LocalDateTime.now());
        given(inboundInterfaceRepository.findById(1L)).willReturn(Optional.of(inboundInterface));

        processor.process(1L);

        verify(inboundExpectationRepository, never()).save(any());
        assertThat(inboundInterface.getStatus()).isEqualTo(InboundInterfaceStatus.FAILED);
    }

    @Test
    @DisplayName("이미 처리된 인터페이스는 건너뛴다")
    void skipsNotPending() {
        InboundInterface inboundInterface = pendingInterface();
        inboundInterface.markDone();
        given(inboundInterfaceRepository.findById(1L)).willReturn(Optional.of(inboundInterface));

        processor.process(1L);

        verify(inboundExpectationRepository, never()).save(any());
    }

    @Test
    @DisplayName("예상하지 못한 오류도 재시도 횟수에 포함한다")
    void countsUnexpectedFailure() {
        InboundInterface inboundInterface = pendingInterface();
        given(inboundInterfaceRepository.findById(1L)).willReturn(Optional.of(inboundInterface));

        processor.recordUnexpectedFailure(1L, "DB 연결 실패");

        assertThat(inboundInterface.getRetryCount()).isEqualTo(1);
        assertThat(inboundInterface.getFailReason()).isEqualTo("처리 중 오류: DB 연결 실패");
    }

    private InboundInterface pendingInterface() {
        InboundExpectationRequest request = new InboundExpectationRequest(
                "ASN-0001", "SUP-001", LocalDateTime.of(2026, 9, 22, 6, 0),
                List.of(new InboundExpectationRequest.Line("SKU-0001", 200), new InboundExpectationRequest.Line("SKU-0003", 50))
        );
        return new InboundInterface("ASN-0001", jsonMapper.writeValueAsString(request), LocalDateTime.now());
    }
}
