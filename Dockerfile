# Stage 1: Build the Spring Boot application
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app

# Copy Maven configuration and source code
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY src ./src

# Build the executable JAR
RUN chmod +x mvnw && ./mvnw -B -DskipTests clean package

# Stage 2: Run the application
FROM eclipse-temurin:25-jre

WORKDIR /app

# Copy the JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Spring Boot application port
EXPOSE 8080

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]
