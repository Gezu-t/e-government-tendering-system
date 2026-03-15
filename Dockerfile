# Shared multi-stage Dockerfile for all Spring Boot services
# Usage: docker build --build-arg SERVICE_NAME=tender-service --build-arg SERVICE_PORT=8082 .

ARG SERVICE_NAME=app
ARG SERVICE_PORT=8080

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY common-util/pom.xml common-util/
COPY app-config-data/pom.xml app-config-data/
ARG SERVICE_NAME
COPY ${SERVICE_NAME}/pom.xml ${SERVICE_NAME}/
# Download dependencies first (cached layer)
RUN mvn dependency:go-offline -pl ${SERVICE_NAME} -am -q || true
# Copy source and build
COPY common-util/ common-util/
COPY app-config-data/ app-config-data/
COPY ${SERVICE_NAME}/ ${SERVICE_NAME}/
RUN mvn package -pl ${SERVICE_NAME} -am -DskipTests -q

# Stage 2: Run
FROM eclipse-temurin:17-jre-alpine
ARG SERVICE_NAME
ARG SERVICE_PORT
ENV SERVICE_NAME=${SERVICE_NAME}
ENV SERVER_PORT=${SERVICE_PORT}
WORKDIR /app
RUN addgroup -S egov && adduser -S egov -G egov
COPY --from=builder /build/${SERVICE_NAME}/target/*.jar app.jar
RUN chown -R egov:egov /app
USER egov
EXPOSE ${SERVICE_PORT}
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget -qO- http://localhost:${SERVER_PORT}/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
