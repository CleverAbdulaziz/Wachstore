FROM openjdk:17-jdk-slim

# Set working directory inside container
WORKDIR /app

# Copy built JAR into container
COPY target/wristwatch-shop-1.0.0.jar app.jar

# Optional: create uploads folder inside container
RUN mkdir -p /app/uploads

# Expose port 8080 (local development)
EXPOSE 8080

# Use dynamic port from Azure, fallback to 8080 locally
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]
