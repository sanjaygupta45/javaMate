# =============================================================================
# JavaMate - Multi-stage Dockerfile for Cloud Run
# =============================================================================
# Stage 1: Build stage (optional - for CI/CD that doesn't pre-build)
# Stage 2: Runtime stage - optimized for Cloud Run
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1: Build (uncomment if you want to build inside Docker)
# -----------------------------------------------------------------------------
# FROM eclipse-temurin:21-jdk-alpine AS builder
# WORKDIR /app
# COPY . .
# RUN ./mvnw clean package -DskipTests

# -----------------------------------------------------------------------------
# Stage 2: Runtime - Optimized for Cloud Run
# -----------------------------------------------------------------------------
# Using Debian-slim instead of Alpine for gRPC/Netty native library compatibility
FROM eclipse-temurin:21-jre-jammy

# Install dumb-init for proper signal handling in containers
RUN apt-get update && apt-get install -y --no-install-recommends dumb-init wget && rm -rf /var/lib/apt/lists/*

# Create non-root user for security
RUN groupadd -g 1001 javamate && \
    useradd -u 1001 -g javamate -s /bin/bash -m javamate

WORKDIR /app

# Copy jar file from local build (CI/CD builds before Docker)
COPY --chown=javamate:javamate target/*.jar app.jar

# Switch to non-root user
USER javamate

# Cloud Run uses PORT env variable (default 8080)
ENV PORT=8080
EXPOSE ${PORT}

# Health check for Cloud Run
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT}/mate/actuator/health || exit 1

# JVM optimizations for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.backgroundpreinitializer.ignore=true"

# Use dumb-init to handle PID 1 responsibilities
ENTRYPOINT ["dumb-init", "--"]
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
