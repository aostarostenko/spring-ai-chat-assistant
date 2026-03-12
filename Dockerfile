# Stage 1: Build the application using JDK 25
# We use the Eclipse Temurin image for JDK 25 and manually set up Maven
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

# Install Maven in the alpine build environment
RUN apk add --no-cache maven

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime environment (stays the same)
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Optimization for Virtual Threads (Java 25)
ENTRYPOINT ["java", "-Dspring.threads.virtual.enabled=true", "-jar", "app.jar"]