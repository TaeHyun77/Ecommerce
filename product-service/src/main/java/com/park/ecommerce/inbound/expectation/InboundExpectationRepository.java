package com.park.ecommerce.inbound.expectation;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface InboundExpectationRepository extends JpaRepository<InboundExpectation, Long> {
    // 같은 입고 확정이 동시에 들어와도 한 요청만 재고를 반영하도록 행을 잠근다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from InboundExpectation e where e.asnNo = :asnNo")
    Optional<InboundExpectation> findByAsnNoForUpdate(String asnNo);
}
