# Multi-stage build for modern-claims-service
# Stage 1: build with a full JDK
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    apk add --no-cache maven && \
    mvn -q -DskipTests package

# Stage 2: run on a slim JRE, non-root user
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S claims && adduser -S claims -G claims
WORKDIR /app
COPY --from=build /workspace/target/modern-claims-service-*.jar app.jar
USER claims
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
