package com.banking.notification.repository;

import com.banking.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Fetch latest notifications for a specific customer
    List<Notification> findByCustomerIdOrderByCreatedAtDesc(String customerId);
}
