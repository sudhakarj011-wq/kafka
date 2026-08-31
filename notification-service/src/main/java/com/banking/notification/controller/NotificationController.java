package com.banking.notification.controller;

import com.banking.notification.entity.Notification;
import com.banking.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * NotificationController — REST API Layer
 *
 * Delegates all queries to NotificationService (service layer).
 * Controller's job: handle HTTP, validate input, return response.
 * Service's job: business logic and data access.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * GET /api/notifications/{customerId}
     * Returns all notifications for a customer, most recent first.
     * Called by Angular frontend to display notifications on the dashboard.
     */
    @GetMapping("/{customerId}")
    public ResponseEntity<List<Notification>> getNotifications(@PathVariable String customerId) {
        log.info("API: Fetching notifications for customer {}", customerId);
        return ResponseEntity.ok(notificationService.getNotificationsForCustomer(customerId));
    }
}
