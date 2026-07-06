# =====================================================================
#  Dockerfile for PMIS Ticket Service  (Spring Boot 3.2.5 / Java 17)
#
#  The ticket-service zip did NOT ship with a Dockerfile, so this one is
#  provided by the deployment package. It mirrors the activity-workflow
#  Dockerfile (multi-stage Maven build -> slim JRE runtime).
#
#  HOW TO USE
#  ----------
#  1. Copy this file INTO the ticket-service source folder, next to pom.xml,
#     and rename it to exactly:   Dockerfile
#         PMIS-Ticket-service-dev/PMIS-Ticket-service-dev/Dockerfile
#  2. Build from inside that folder (see scripts/build-images.sh).
#
#  The app listens on container port 8081 with context-path /ticket-service.
# =====================================================================

# ---- build stage --------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# cache dependencies first
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src src
RUN mvn -B -q -DskipTests package

# ---- runtime stage ------------------------------------------------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# non-root user
RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /workspace/target/*.jar /app/app.jar

EXPOSE 8081
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
