# Microservices & Kafka Technical Interview Guide
*Based on the Real-Time Banking System Implementation*

This document provides high-value interview questions and answers directly mapped to the code implemented in this project. Use this to explain the architecture in technical interviews.

---

## 1. Apache Kafka Fundamentals

### Q: Why did we choose Kafka over RabbitMQ or synchronous HTTP calls for the Payment System?
**A:** We used Kafka because a single banking transaction involves multiple independent business domains (Fraud, Audit, Analytics, Notifications). 
- If we used HTTP (OpenFeign), the Payment Service would have to wait for all 4 services to finish, causing high latency and tight coupling.
- If we used RabbitMQ (traditional message queue), messages are deleted after consumption. Kafka is an immutable **append-log**, allowing us to replay events (Event Sourcing) and enabling multiple independent consumer groups to read the same message.

### Q: How do Consumer Groups work in our architecture?
**A:** Consumer groups allow us to achieve a **Publish-Subscribe (Pub/Sub)** model while maintaining horizontal scalability.
In our `payment-events` topic, we defined **four** different consumer groups inside their `application.yml` files:
1. `notification-group`
2. `fraud-group`
3. `audit-group`
4. `analytics-group`
Because the group IDs are different, Kafka delivers a full copy of the event to each group. The Fraud service processes the event entirely completely parallel to the Notification service.

### Q: If we spin up 3 instances of the Notification Service, will the customer get 3 emails for the same transfer?
**A:** No. Because all 3 instances share the *same* `group-id` (`notification-group`), Kafka automatically load balances the processing across the instances. If our topic has 4 partitions, one instance might get 2 partitions, and the others get 1 partition each. A single message is delivered to only exactly **one** consumer within a given group.

---

## 2. Dealing with Exactly-Once Processing (Idempotency)

### Q: Kafka guarantees "At-Least-Once" delivery by default. How did you prevent duplicate processing (e.g., sending the same Notification twice)?
**A:** We implemented **Idempotent Consumers**. 
In our `NotificationConsumer.java`, when we receive a `PaymentEventDto`, we don't just blindly send an email. 
1. We extract the unique `eventId` from the Kafka message.
2. We query the `processed_events` MySQL table: `existsByEventId(eventId)`.
3. If it exists, we drop the message and return (duplicate detected).
4. If it does not exist, we save the Notification and the `eventId` in the same `@Transactional` boundary. This guarantees that exactly-once semantics are strictly enforced at the database level regardless of network retries.

---

## 3. Distributed Transactions and The Outbox Pattern

### Q: How did you ensure data consistency between saving the transaction in the MySQL database and publishing the event to Kafka? (The Dual-Write Problem)
**A:** This is a classic microservices problem. If we save the transaction to the database, and then Java crashes *before* calling `kafkaTemplate.send()`, the money is moved but no notification/fraud check ever happens.

We solved this using the **Transactional Outbox Pattern** in the `PaymentService`:
1. We do not call `KafkaTemplate` directly inside our business logic.
2. Instead, inside the `@Transactional` method, we save the `Transaction` entity AND we save an `OutboxEvent` (containing the JSON payload) to the same database. Since it's standard ACID SQL, if the database crashes, both rollback together.
3. A separate asynchronous `@Scheduled` job (`OutboxPublisher.java`) continuously polls the `outbox_events` table for records marked `PENDING`.
4. It reads the record, publishes to Kafka, and upon Kafka's successful ACK, marks it as `SENT`.
This guarantees **At-Least-Once** publishing without using heavy distributed transactions (2PC/XA).

---

## 4. Error Handling and The Dead Letter Topic (DLT)

### Q: What happens if the Notification Service throws a NullPointerException while processing a message? Does it block the partition?
**A:** If a consumer throws an exception, standard Kafka behavior is to infinitely retry, blocking the partition (poison pill). 
To solve this, we configure a **Dead Letter Topic (DLT)**. 
Using Spring Kafka's `@RetryableTopic` or manual ErrorHandlers, if a message fails processing `N` times, it is automatically forwarded to `payment-events-dlt`. This moves the bad message out of the way so normal processing continues, and developers can manually inspect the DLT later to fix the bug and replay the event.

---

## 5. Security & Gateway

### Q: How is security handled in this distributed system?
**A:** Security is centralized at the **API Gateway** to prevent duplication of logic across 8 microservices.
1. The `AccountService` exposes a `/auth/login` endpoint that generates a tamper-proof JWT token.
2. The Angular UI stores this token and sends it in the `Authorization: Bearer <token>` header (via the `jwt.interceptor.ts`).
3. The Spring Cloud Gateway has a `JwtAuthFilter` that intercepts *all* incoming requests. It validates the JWT signature. If valid, it forwards the request to the downstream microservices. The microservices sit inside a protected Docker internal network and implicitly trust requests coming from the Gateway.

---

## 6. Loose Coupling and Domain Driven Design

### Q: Why did you copy the `PaymentEventDto.java` class into every consuming microservice instead of just putting it in a shared Maven `.jar`?
**A:** This is a vital Microservices best practice. If we put the DTO in a shared `banking-core.jar`, and the Payment Service wants to change the object (e.g., adding a field), we have to update the jar version and re-deploy *every single microservice* simultaneously. This creates a Monolithic Deployment pipeline.
By duplicating the simple POJO (adhering to the "Loose Coupling" principle over "DRY"), the Notification service can safely ignore new fields it doesn't care about, and we can deploy the Payment service entirely independently of the Audit service.

---

## Conclusion
This architecture proves a deep understanding of asynchronous, event-driven banking flows capable of handling thousands of TPS while guaranteeing absolute consistency and fault tolerance.
