# 📚 Kafka Concepts — Practical Banking Examples

> Every concept is explained with: Real Example → Code → Interview Answer

---

## 1. 🎯 Topic

### What is a Topic?
A **Topic** is like a named channel or mailbox in Kafka where messages are published.

```
Producer ──publishes──► [  payment-events TOPIC  ] ──consumed by──► Consumer
```

### Real Banking Example
```
When Customer A transfers ₹10,000 to Customer B:

Payment Service publishes → "payment-events" topic
                                     │
                    ┌────────────────┼─────────────────┐
                    ▼                ▼                  ▼
           Notification      Fraud Detection        Audit Service
              Service            Service
```

### In Our Project
```java
// Kafka Topic Configuration — KafkaTopicConfig.java
@Bean
public NewTopic paymentEventsTopic() {
    return TopicBuilder.name("payment-events")
            .partitions(4)      // 4 partitions for parallel processing
            .replicas(1)        // 1 replica (dev). Use 3 in production!
            .build();
}
```

### Interview Answer
> **Q: What is a Kafka Topic?**
> **A:** A Topic is a named, ordered, immutable log of records. Producers write to it, and multiple consumer groups can independently read from it. In our banking system, `payment-events` is the topic. Every payment event is appended to it like a ledger — nothing is ever deleted (configurable retention). This is why Kafka is ideal for banking: auditability is built-in.

---

## 2. ⚙️ Producer

### What is a Producer?
A component that **publishes** messages to a Kafka topic.

### Real Banking Example
```
Payment Service
      │
      │ After successful debit + credit (DB committed)
      ▼
publish PaymentEvent to "payment-events"
```

### In Our Project
```java
// KafkaProducerService.java (Payment Service)
@Service
public class KafkaProducerService {

    @Autowired
    private KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    public void publishPaymentEvent(PaymentEvent event) {
        // KEY = transactionId (for ordering — same txn goes to same partition)
        // VALUE = PaymentEvent JSON
        kafkaTemplate.send("payment-events",
                           event.getTransactionId(),  // KEY
                           event);                    // VALUE

        log.info("Published PaymentEvent: {}", event.getTransactionId());
    }
}
```

### Interview Answer
> **Q: Why does Payment Service use Kafka and not direct REST calls?**
> **A:** After the DB transaction commits (debit + credit), we need to notify multiple services: notification, fraud, audit, analytics. If we used REST calls, Payment Service would need to know about all 4 services, creating tight coupling. If one service is down, the payment would fail. With Kafka, Payment Service only knows about one topic. It publishes the event and its job is done. All 4 consumers independently process the event. This is **Loose Coupling** and **Asynchronous Communication**.

---

## 3. 👥 Consumer Groups

### What are Consumer Groups?
A **Consumer Group** is a set of consumers that share the work of consuming a topic.

### The MOST Important Concept for Interviews

**Different groups = each group gets EVERY message**
```
"payment-events" topic
          │
    ┌─────┴──────┬──────────────┬───────────────┐
    ▼            ▼              ▼               ▼
notification-  fraud-       audit-         analytics-
   group        group        group           group
    │            │              │               │
Notification  Fraud Check    Audit Log    Analytics Update
  Service       Service       Service       Service

RESULT: All 4 services receive the SAME PaymentEvent
```

**Same group = messages are SHARED (load balanced)**
```
If we run 2 instances of Notification Service in "notification-group":

"payment-events" (4 partitions)
    P0  P1  P2  P3
    │   │   │   │
    ├───┤   ├───┤
    ▼   ▼   ▼   ▼
 Instance1   Instance2
(P0,P1)     (P2,P3)

RESULT: Each instance processes DIFFERENT messages = parallelism
```

### In Our Project
```java
// notification-service
@KafkaListener(topics = "payment-events", groupId = "notification-group")
public void processNotification(PaymentEvent event) { /* ... */ }

// fraud-service
@KafkaListener(topics = "payment-events", groupId = "fraud-group")
public void processFraud(PaymentEvent event) { /* ... */ }

// audit-service
@KafkaListener(topics = "payment-events", groupId = "audit-group")
public void processAudit(PaymentEvent event) { /* ... */ }

// analytics-service
@KafkaListener(topics = "payment-events", groupId = "analytics-group")
public void processAnalytics(PaymentEvent event) { /* ... */ }
```

### Interview Answer
> **Q: Why do you have 4 different consumer groups?**
> **A:** Because each service needs to independently process every payment event. If they were in the same group, each event would be processed by only ONE service (load balanced). By using separate groups (`notification-group`, `fraud-group`, `audit-group`, `analytics-group`), Kafka delivers each event to ALL four groups. Each group maintains its own offset — so if the Notification Service is down, it doesn't affect the Fraud Service's progress.

---

## 4. 📂 Partitions

### What are Partitions?
A topic is divided into ordered **partitions** for parallelism and scalability.

```
"payment-events" topic (4 partitions)
┌─────────────────────────────────────────────┐
│  Partition 0: [EVT1] [EVT5] [EVT9]  ...    │
│  Partition 1: [EVT2] [EVT6] [EVT10] ...    │
│  Partition 2: [EVT3] [EVT7] [EVT11] ...    │
│  Partition 3: [EVT4] [EVT8] [EVT12] ...    │
└─────────────────────────────────────────────┘
```

### How Messages Are Distributed
```java
// KEY determines which partition a message goes to:
// Partition = hash(key) % numPartitions

kafkaTemplate.send("payment-events",
                   event.getTransactionId(), // KEY = "TXN1001"
                   event);

// hash("TXN1001") % 4 = 2 → always goes to Partition 2
// This guarantees ORDERING: all events for TXN1001 are in same partition
```

### Real Banking Use Case
```
Customer ACC1001 makes 3 transfers:
  TXN001 → hash("TXN001") % 4 = 1 → Partition 1
  TXN002 → hash("TXN002") % 4 = 1 → Partition 1
  TXN003 → hash("TXN003") % 4 = 3 → Partition 3

Within partitions, ORDER is guaranteed.
Across partitions, order is NOT guaranteed (but for different transactions, we don't care).
```

### Interview Answer
> **Q: Why use partitions? How many should you use?**
> **A:** Partitions enable parallel processing. If a topic has 4 partitions, up to 4 consumers in a group can process messages simultaneously. In production, the number of partitions should match your expected consumer parallelism. We use `transactionId` as the key so all events for the same transaction always go to the same partition — guaranteeing ordering for that transaction.

---

## 5. 📍 Offset

### What is an Offset?
An **offset** is a unique sequential ID for each message within a partition. Kafka uses offsets to track which messages have been consumed.

```
Partition 0:
┌────┬────┬────┬────┬────┐
│  0 │  1 │  2 │  3 │  4 │  ← Offsets
│EVT1│EVT5│EVT9│... │... │
└────┴────┴────┴────┴────┘
                 ▲
         notification-group consumed up to offset 2
         fraud-group consumed up to offset 4 (ahead!)
```

### Real Banking Example — Consumer Down Scenario
```
Time 10:00 → Payment TXN001 published (offset 0)
Time 10:01 → Notification Service DOWN ❌

Kafka stores: notification-group committed offset = -1 (nothing consumed)

Time 10:30 → Notification Service comes BACK UP ✅
Kafka says: "notification-group last consumed offset = -1"
             "Start from offset 0"
             
Notification Service processes TXN001 ✅
(even though it was published 30 minutes ago!)
```

### Interview Answer
> **Q: What happens to events if Notification Service is down?**
> **A:** Nothing is lost! Kafka is a persistent log. When Notification Service comes back up, it reads from where it last committed its offset. If it was at offset 5 when it crashed, it resumes from offset 6. The payment is NOT affected because Payment Service and the Kafka topic don't care whether consumers are alive. This is the core value of **decoupling** — the producer and consumer are completely independent.

---

## 6. 🔑 Message Key

### What is a Key?
A key attached to each Kafka message that determines:
1. Which partition it goes to (by hash)
2. Ordering guarantee (same key → same partition → ordered)

### In Our Project
```java
// We use transactionId as key
kafkaTemplate.send(
    "payment-events",      // topic
    event.getTransactionId(), // key → determines partition
    event               // value (PaymentEvent JSON)
);
```

### Why It Matters for Banking
```
If order matters (e.g., multiple events for same transaction):
  PAYMENT_INITIATED  (TXN1001) → Partition 2
  PAYMENT_SUCCESS    (TXN1001) → Partition 2 ← SAME partition!
  PAYMENT_REFUNDED   (TXN1001) → Partition 2 ← SAME partition!

All events for TXN1001 are processed IN ORDER by the consumer.
```

---

## 7. 🔁 Idempotency

### The Problem
In distributed systems, the same message can be delivered **more than once** (at-least-once delivery guarantee). This is DANGEROUS in banking!

```
Scenario:
  1. Kafka delivers PaymentSuccessful TXN001 to Notification Service
  2. Notification Service processes it → saves notification in DB
  3. Before committing offset → Notification Service CRASHES
  4. Kafka delivers PaymentSuccessful TXN001 AGAIN
  5. Notification Service processes it AGAIN → 2 notifications! ❌
```

### The Solution — processed_events table
```java
// Before processing any event:
@Transactional
public void processEvent(PaymentEvent event) {

    // Step 1: Check if this event was already processed
    if (processedEventRepository.existsByEventIdAndConsumerName(
            event.getEventId(), "notification-service")) {
        log.warn("Duplicate event detected: {}. Skipping.", event.getEventId());
        return;  // Idempotency — skip duplicate!
    }

    // Step 2: Process the event
    createNotification(event);

    // Step 3: Save event ID to prevent future duplicates
    processedEventRepository.save(ProcessedEvent.builder()
            .eventId(event.getEventId())
            .consumerName("notification-service")
            .processedAt(LocalDateTime.now())
            .build());
}
```

### Interview Answer
> **Q: How do you handle duplicate Kafka messages in banking?**
> **A:** We implement the **Idempotent Consumer pattern** using a `processed_events` table. Before processing any event, we check if its `eventId` already exists in this table. If yes, we skip it. If no, we process it and then save the `eventId`. Both steps happen in the same `@Transactional` block, so either both succeed or both fail. This prevents double notifications, double fraud checks, and double audit entries.

---

## 8. 📤 Outbox Pattern

### The Problem — Dual-Write
```
@Transactional
public void transfer(PaymentRequest request) {
    debitSender();          // DB write ✅
    creditReceiver();       // DB write ✅
    saveTransaction();      // DB write ✅
    // DB COMMITS HERE ✅

    kafkaTemplate.send(event); // ← What if THIS fails? ❌
    // DB committed but Kafka event NOT sent!
    // Inconsistency: money moved but no notification!
}
```

### The Solution — Outbox Pattern
```
@Transactional
public void transfer(PaymentRequest request) {
    debitSender();           // DB write ✅
    creditReceiver();        // DB write ✅
    saveTransaction();       // DB write ✅
    saveToOutbox(event);     // DB write ✅ (same transaction!)
    // DB COMMITS ALL 4 TOGETHER ✅
}

// Separate scheduled job (every 5 seconds):
@Scheduled(fixedDelay = 5000)
public void publishOutboxEvents() {
    List<OutboxEvent> unpublished = outboxRepository.findByStatus(PENDING);
    for (OutboxEvent event : unpublished) {
        kafkaTemplate.send(event);  // publish to Kafka
        event.setStatus(SENT);     // mark as done
        outboxRepository.save(event);
    }
}
```

### Interview Answer
> **Q: What is the Transactional Outbox Pattern and why do you need it?**
> **A:** The Outbox Pattern solves the "dual-write problem" in distributed systems. In a naive implementation, we write to the DB and then publish to Kafka — but these are two separate operations. If Kafka publish fails after the DB commits, our system is in an inconsistent state (money transferred, no event published). The Outbox Pattern saves the Kafka event in the same DB transaction as the business data. A separate poller reads the outbox and publishes to Kafka. Now atomicity is guaranteed by the DB transaction.

---

## 9. ❌ Dead Letter Topic (DLT)

### What is DLT?
When a consumer fails to process a message even after all retries, Kafka moves it to a **Dead Letter Topic** for manual inspection.

```
payment-events
      │
      ▼
Notification Service fails (malformed event? service bug?)
      │
  Retry #1 → fail
  Retry #2 → fail
  Retry #3 → fail
      │
      ▼
payment-events.DLT ← Dead Letter Topic
(message stored here for manual inspection/replay)
```

### In Our Project
```java
@RetryableTopic(
    attempts = "3",
    backoff = @Backoff(delay = 1000, multiplier = 2),
    dltTopicSuffix = ".DLT"
)
@KafkaListener(topics = "payment-events", groupId = "notification-group")
public void processNotification(PaymentEvent event) {
    // If this throws 3 times → message goes to "payment-events.DLT"
}
```

---

## 10. 🔄 Replication (Production)

### Why Replication Matters
```
Production Kafka (3 brokers, replication factor 3):

Broker 1: [P0_Leader] [P1_Follower] [P2_Follower]
Broker 2: [P0_Follower] [P1_Leader] [P2_Follower]
Broker 3: [P0_Follower] [P1_Follower] [P2_Leader]

If Broker 1 dies → Kafka automatically elects P0's follower as new leader
ZERO data loss, ZERO downtime!
```

### Interview Answer
> **Q: How does Kafka ensure no data loss in production?**
> **A:** Kafka uses replication. Each partition has a leader and multiple replicas (followers) on different brokers. Writes go to the leader and are replicated to followers. If the leader fails, a follower is automatically elected as the new leader. With `replication.factor=3` and `min.insync.replicas=2`, even if one broker fails completely, no messages are lost. In our dev environment we use `replicas(1)`, but in production we always use `replicas(3)`.
