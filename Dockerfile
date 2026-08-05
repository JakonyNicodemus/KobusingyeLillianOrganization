# Use a small, fast Java 17 image
FROM eclipse-temurin:17-alpine

# Set the working directory
WORKDIR /app

# Copy everything from your project
COPY . /app

# List files to verify they were copied (for debugging)
RUN echo "=== Files in /app ===" && ls -la /app
RUN echo "=== Files in /app/frontend ===" && ls -la /app/frontend || echo "frontend folder not found!"

# Create classes directory
RUN mkdir -p /app/classes

# Compile Java files
RUN javac -d /app/classes /app/Backend/src/com/ems/*.java

# Set classpath
ENV CLASSPATH=/app/classes

# Expose port
EXPOSE 8080

# Start the application
CMD ["java", "-cp", "/app/classes", "com.ems.Main"]