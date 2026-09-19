package com.park.ecommerce.inbound.reception;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InboundInterfaceRepository extends JpaRepository<InboundInterface, Long> {
    Optional<InboundInterface> findByAsnNo(String asnNo);

    List<InboundInterface> findAllByStatusOrderByIdDesc(InboundInterfaceStatus status);

    @Query("select i.id from InboundInterface i where i.status = :status and i.nextRetryAt <= :now order by i.id")
    List<Long> findProcessTargetIds(InboundInterfaceStatus status, LocalDateTime now, Pageable pageable);
}
