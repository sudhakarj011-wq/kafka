package com.banking.notification.repository;

import com.banking.notification.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {

    // Fast check for idempotency
    boolean existsByEventId(String eventId);
}
