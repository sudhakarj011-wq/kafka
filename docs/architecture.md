# 🏦 Banking Payment System — Complete Architecture

## System Overview

```
                    ┌─────────────────────────┐
                    │      ANGULAR 17+         │
                    │   (localhost:4200)        │
                    │  Login | Dashboard        │
                    │  Transfer | History       │
                    │  Notifications            │
                    └────────────┬────────────-┘
                                 │ HTTP + JWT
                                 ▼
                    ┌────────────────────────-─┐
                    │       API GATEWAY         │
                    │   (localhost:8080)        │
                    │  JWT Validation           │
                    │  Route → Services         │
                    │  Spring Cloud Gateway     │
                    └────────────┬─────────────┘
                                 │
              ┌──────────────────┼────────────────────┐
              │                  │                     │
              ▼                  ▼                     ▼
   ┌─────────────────┐ ┌─────────────────┐  ┌───────────────────┐
   │  ACCOUNT SVC    │ │  PAYMENT SVC    │  │   ANALYTICS SVC   │
   │  port: 8081     │ │  port: 8082     │  │   port: 8086      │
   │  MySQL:         │ │  MySQL:         │  │   MySQL:          │
   │  account_db     │ │  payment_db     │  │   analytics_db    │
   └─────────────────┘ └────────┬────────┘  └───────────────────┘
          ▲                     │                     ▲
          │ OpenFeign           │ @Transactional       │
          │ (debit/credit)      │ commit               │ kafka consumer
          └─────────────────────┘                     │
                                 │                    │
                                 ▼ publish event      │
                    ┌────────────────────────┐        │
                    │         KAFKA          │        │
                    │   topic: payment-events│        │
                    │   partitions: 4        │        │
                    │   (localhost:9092)     │        │
                    └────────────┬───────────┘        │
                                 │                    │
          ┌──────────────────────┼─────────────────┐  │
          │                      │                 │  │
          ▼                      ▼                 ▼  ▼
 ┌────────────────┐  ┌──────────────────┐  ┌──────────────────┐
 │ NOTIFICATION   │  │  FRAUD SERVICE   │  │  AUDIT SERVICE   │
 │   SERVICE      │  │  port: 8084      │  │  port: 8085      │
 │  port: 8083    │  │  MySQL: fraud_db │  │  MySQL: audit_db │
 │  MySQL:        │  │  group:          │  │  group:          │
 │  notif_db      │  │  fraud-group     │  │  audit-group     │
 │  group:        │  └──────────────────┘  └──────────────────┘
 │  notif-group   │              │
 └────────────────┘              │ to analytics-group
                                 └─────────────────────────────►
                                                          (see above)

                    ┌────────────────────────┐
                    │     EUREKA SERVER       │
                    │   (localhost:8761)      │
                    │  Service Registry       │
                    └────────────────────────┘
                    All services register here

                    ┌────────────────────────┐
                    │       KAFKA UI          │
                    │   (localhost:8090)      │
                    │  Monitor topics,        │
                    │  messages, consumer lag │
                    └────────────────────────┘
```

---

## Service Port Map

| Service | Port | Database | Purpose |
|---|---|---|---|
| API Gateway | 8080 | — | Entry point, JWT auth, routing |
| Account Service | 8081 | account_db | Account CRUD, balance management |
| Payment Service | 8082 | payment_db | Transfer logic, Kafka producer |
| Notification Service | 8083 | notification_db | Kafka consumer, notifications |
| Fraud Service | 8084 | fraud_db | Kafka consumer, fraud detection |
| Audit Service | 8085 | audit_db | Kafka consumer, audit logs |
| Analytics Service | 8086 | analytics_db | Kafka consumer, stats |
| Eureka Server | 8761 | — | Service discovery |
| Kafka | 9092 | — | Message broker |
| Kafka UI | 8090 | — | Monitoring dashboard |
| MySQL | 3306 | all DBs | Relational database |
| Angular (dev) | 4200 | — | Frontend |

---

## Kafka Event Flow

```
Payment Service
      │
      │ 1. POST /api/payments/transfer
      │
      ▼
┌─── @Transactional block ──────────────────────┐
│  a. Validate fromAccount (Feign → Account Svc) │
│  b. Validate toAccount (Feign → Account Svc)   │
│  c. Check balance (InsufficientBalance?)        │
│  d. Create transaction (status: INITIATED)      │
│  e. Debit fromAccount (Feign → Account Svc)    │
│  f. Credit toAccount (Feign → Account Svc)     │
│  g. Update transaction (status: SUCCESS)        │
│  h. DB COMMIT ✅                               │
└───────────────────────────────────────────────┘
      │
      │ 2. Publish PaymentEvent to Kafka
      ▼
{
  "eventId": "EVT-abc123",
  "eventType": "PAYMENT_SUCCESSFUL",
  "transactionId": "TXN-xyz789",
  "fromAccount": "ACC1A2B3C",
  "toAccount": "ACCDEF456",
  "amount": 10000.00,
  "timestamp": "2026-08-27T20:30:00"
}
      │
      ▼
Kafka topic: payment-events (4 partitions)
      │ key: transactionId → determines partition
      │
      ├──► notification-group → Notification Service → notifications table
      ├──► fraud-group        → Fraud Service        → fraud_transactions table
      ├──► audit-group        → Audit Service        → audit_logs table
      └──► analytics-group    → Analytics Service    → payment_summary table
```

---

## Database Schema (All Services)

### account_db.accounts
```sql
CREATE TABLE accounts (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(20) UNIQUE NOT NULL,
    customer_id    VARCHAR(20) NOT NULL,
    customer_name  VARCHAR(100) NOT NULL,
    balance        DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    status         ENUM('ACTIVE','INACTIVE','SUSPENDED') DEFAULT 'ACTIVE',
    created_at     DATETIME,
    updated_at     DATETIME
);
```

### payment_db.transactions
```sql
CREATE TABLE transactions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id  VARCHAR(36) UNIQUE NOT NULL,
    from_account    VARCHAR(20) NOT NULL,
    to_account      VARCHAR(20) NOT NULL,
    amount          DECIMAL(15,2) NOT NULL,
    status          ENUM('INITIATED','SUCCESS','FAILED') DEFAULT 'INITIATED',
    description     VARCHAR(255),
    created_at      DATETIME,
    updated_at      DATETIME
);
```

### notification_db.notifications
```sql
CREATE TABLE notifications (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id     VARCHAR(20) NOT NULL,
    transaction_id  VARCHAR(36) NOT NULL,
    message         TEXT NOT NULL,
    status          ENUM('UNREAD','READ') DEFAULT 'UNREAD',
    created_at      DATETIME
);
```

### notification_db.processed_events (Idempotency)
```sql
CREATE TABLE processed_events (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id        VARCHAR(50) NOT NULL,
    consumer_name   VARCHAR(50) NOT NULL,
    processed_at    DATETIME,
    UNIQUE KEY uk_event_consumer (event_id, consumer_name)
);
```

### fraud_db.fraud_transactions
```sql
CREATE TABLE fraud_transactions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id  VARCHAR(36) UNIQUE NOT NULL,
    risk_level      ENUM('LOW_RISK','MEDIUM_RISK','HIGH_RISK') NOT NULL,
    reason          VARCHAR(255),
    created_at      DATETIME
);
```

### audit_db.audit_logs
```sql
CREATE TABLE audit_logs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id        VARCHAR(50) UNIQUE NOT NULL,
    transaction_id  VARCHAR(36) NOT NULL,
    event_type      VARCHAR(50) NOT NULL,
    payload         LONGTEXT NOT NULL,
    created_at      DATETIME
);
```

### payment_db.outbox_events (Outbox Pattern)
```sql
CREATE TABLE outbox_events (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id        VARCHAR(50) UNIQUE NOT NULL,
    aggregate_type  VARCHAR(50) NOT NULL,
    aggregate_id    VARCHAR(50) NOT NULL,
    event_type      VARCHAR(50) NOT NULL,
    payload         LONGTEXT NOT NULL,
    status          ENUM('PENDING','SENT','FAILED') DEFAULT 'PENDING',
    created_at      DATETIME,
    sent_at         DATETIME
);
```
