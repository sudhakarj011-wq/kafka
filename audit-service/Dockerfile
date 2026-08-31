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

COPY --from=builder /app/target/audit-service-1.0.0.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]
