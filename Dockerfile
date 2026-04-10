
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Copy jar file to container
COPY target/*.jar app.jar

# Expose application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java","-jar","app.jar"]