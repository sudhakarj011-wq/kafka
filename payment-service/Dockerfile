# ============================================================
# Payment Service — Dockerfile
# Multi-stage build: Maven compiler → JRE runtime
# ============================================================

# Stage 1 — Build
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2 — Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
RUN addgroup -S bankinggroup && adduser -S bankinguser -G bankinggroup
USER bankinguser

COPY --from=builder /app/target/payment-service-1.0.0.jar app.jar

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD wget -q --spider http://localhost:8082/actuator/health || exit 1

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
