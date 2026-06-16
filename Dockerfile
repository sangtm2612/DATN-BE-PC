# ─── Stage 1: Build ──────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Build
COPY src ./src
RUN mvn clean package -DskipTests -q

# ─── Stage 2: Runtime ────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Security: run as non-root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Xmx512m", \
  "-Xms256m", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
