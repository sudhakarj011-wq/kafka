# 🏦 Banking Payment System — Getting Started

## Project Structure

```
kafka/
├── account-service/          ← Phase 1 ✅
├── payment-service/          ← Phase 2 (next)
├── notification-service/     ← Phase 6
├── fraud-service/            ← Phase 7
├── audit-service/            ← Phase 8
├── analytics-service/        ← Phase 9
├── eureka-server/            ← Phase 3
├── api-gateway/              ← Phase 4
├── banking-frontend/         ← Phase 11
├── docs/
│   ├── architecture.md       ← System architecture diagrams
│   ├── kafka-concepts.md     ← Kafka concepts with banking examples
│   └── test-data.sql         ← Sample test data
├── init-db.sql               ← MySQL initialization (all databases)
├── docker-compose-phase1.yml ← MySQL Docker Compose (Phase 1)
└── docker-compose.yml        ← Full Docker Compose (Phase 12)
```

---

## Phase 1 — Running Account Service

### Prerequisites
- Java 21
- Maven 3.9+
- Docker Desktop (for MySQL)

---

### Step 1: Start MySQL with Docker

```bash
cd c:\Users\Sudhakar\Desktop\kubernetes\kafka

# Start MySQL container
docker compose -f docker-compose-phase1.yml up -d

# Verify MySQL is running
docker ps
# Expected: banking-mysql container running on port 3306

# Check MySQL logs
docker logs banking-mysql
```

---

### Step 2: Start Account Service

```bash
cd account-service

# Build and run
mvn spring-boot:run

# OR build JAR first
mvn clean package -DskipTests
java -jar target/account-service-1.0.0.jar
```

**Expected startup output:**
```
===========================================
   Account Service Started on port 8081   
   Registered with Eureka: localhost:8761  
===========================================
```

> Note: Eureka connection will fail initially (Eureka server not started yet).
> That's OK for Phase 1 — Account Service still works independently.
> The error log: `Cannot execute request on any known server` is expected.

---

### Step 3: Test APIs with cURL

#### ✅ Create Account 1 (Ravi Kumar — Sender)
```bash
curl -X POST http://localhost:8081/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST001",
    "customerName": "Ravi Kumar",
    "initialBalance": 50000.00
  }'
```

Expected Response (201 CREATED):
```json
{
  "id": 1,
  "accountNumber": "ACC1A2B3C",
  "customerId": "CUST001",
  "customerName": "Ravi Kumar",
  "balance": 50000.00,
  "status": "ACTIVE"
}
```
📝 **Note the accountNumber** — you'll need it for payment testing!

---

#### ✅ Create Account 2 (Priya Sharma — Receiver)
```bash
curl -X POST http://localhost:8081/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST002",
    "customerName": "Priya Sharma",
    "initialBalance": 30000.00
  }'
```

---

#### ✅ Get Account Details
```bash
# Replace ACC1A2B3C with your actual account number
curl http://localhost:8081/api/accounts/ACC1A2B3C
```

---

#### ✅ Get Balance
```bash
curl http://localhost:8081/api/accounts/ACC1A2B3C/balance
```

Expected Response:
```json
{
  "accountNumber": "ACC1A2B3C",
  "customerName": "Ravi Kumar",
  "balance": 50000.00,
  "currency": "INR"
}
```

---

#### ✅ Debit Account (test balance deduction)
```bash
curl -X PUT http://localhost:8081/api/accounts/ACC1A2B3C/debit \
  -H "Content-Type: application/json" \
  -d '{"amount": 10000.00}'
```

Expected: balance reduces from 50000 to 40000

---

#### ✅ Credit Account (test balance addition)
```bash
curl -X PUT http://localhost:8081/api/accounts/ACCDEF456/credit \
  -H "Content-Type: application/json" \
  -d '{"amount": 10000.00}'
```

---

#### ✅ Test Validation — Insufficient Balance
```bash
curl -X PUT http://localhost:8081/api/accounts/ACC1A2B3C/debit \
  -H "Content-Type: application/json" \
  -d '{"amount": 999999.00}'
```

Expected (400 Bad Request):
```json
{
  "status": 400,
  "message": "Insufficient balance in account ACC1A2B3C. Available: ₹40000.00"
}
```

---

#### ✅ Test Validation — Account Not Found
```bash
curl http://localhost:8081/api/accounts/ACCNOTEXIST
```

Expected (404 Not Found):
```json
{
  "status": 404,
  "message": "Account not found: ACCNOTEXIST"
}
```

---

#### ✅ Test Validation — Bean Validation
```bash
curl -X POST http://localhost:8081/api/accounts \
  -H "Content-Type: application/json" \
  -d '{"customerId": "", "customerName": "X", "initialBalance": -100}'
```

Expected (400 Bad Request with field errors):
```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": {
    "customerId": "Customer ID is required",
    "customerName": "Customer name must be between 2-100 characters",
    "initialBalance": "Balance cannot be negative"
  }
}
```

---

### Step 4: Verify in MySQL

```bash
# Connect to MySQL container
docker exec -it banking-mysql mysql -uroot -proot account_db

# View all accounts
SELECT id, account_number, customer_name, balance, status FROM accounts;

# Exit MySQL
exit
```

---

## 🎯 What We Learned in Phase 1

| Concept | Implementation |
|---|---|
| Spring Boot 3.x | Auto-configuration, embedded Tomcat |
| Spring Data JPA | Derived queries, `save()`, `findByAccountNumber()` |
| `@Transactional` | Atomic debit/credit operations |
| BigDecimal | Exact monetary arithmetic (not double!) |
| `@Valid` + Bean Validation | Request body validation |
| `@RestControllerAdvice` | Global exception handling |
| DTOs | Separating API contract from entity |
| Eureka Client | Service registration (ready for Phase 3) |
| Docker | MySQL containerized, persistent volume |

---

## ✅ Phase 1 Complete! Next: Phase 2 — Payment Service

In Phase 2, we will:
1. Create Payment Service on port 8082
2. Add OpenFeign to call Account Service
3. Implement `@Transactional` money transfer
4. Add Transaction entity + MySQL
5. Prepare for Kafka publishing (Phase 5)

---

## 📚 Kafka Interview Questions

See: [`docs/kafka-concepts.md`](docs/kafka-concepts.md) for:
- Topic, Producer, Consumer
- Consumer Groups (the most important concept!)
- Partitions and Offsets
- Idempotency pattern
- Outbox Pattern
- Dead Letter Topic (DLT)
