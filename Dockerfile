FROM eclipse-temurin:17-alpine

WORKDIR /app

# Copy everything
COPY . /app

# Compile Java
RUN cd Backend && javac -d ./classes src/com/ems/*.java

# Install Node.js for possible frontend serving
RUN apk add --no-cache nodejs npm

# Expose port
EXPOSE 8080

# Start the application
CMD ["sh", "-c", "cd Backend && java -cp ./classes com.ems.Main"]