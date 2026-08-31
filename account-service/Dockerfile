# ============================================================
# Account Service — Dockerfile
# Multi-stage build for small production image
#
# Stage 1 (builder): Maven compiles the application → JAR
# Stage 2 (runtime): Only JRE + JAR, no build tools
#
# Result: ~200MB image instead of ~600MB with full JDK+Maven
# ============================================================

# Stage 1 — Build
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom.xml first (for Docker layer caching)
# If only source code changes, Maven dependencies won't re-download
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# ──────────────────────────────────────────────

# Stage 2 — Runtime (lean image)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Non-root user for security best practice
RUN addgroup -S bankinggroup && adduser -S bankinguser -G bankinggroup
USER bankinguser

COPY --from=builder /app/target/account-service-1.0.0.jar app.jar

# Health check — Docker will mark container unhealthy if this fails
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD wget -q --spider http://localhost:8081/actuator/health || exit 1

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
