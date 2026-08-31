package com.banking.payment.repository;

import com.banking.payment.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * Fetch all PENDING outbox events for the scheduler to publish.
     * Ordered by creation time to publish in order.
     */
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxEvent.OutboxStatus status);

    boolean existsByEventId(String eventId);
}
