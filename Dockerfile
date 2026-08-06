FROM eclipse-temurin:17-alpine

WORKDIR /app

# Copy everything
COPY . /app

# List files for debugging
RUN echo "=== Files in /app ===" && ls -la /app
RUN echo "=== Files in /app/frontend ===" && ls -la /app/frontend || echo "frontend folder not found!"

# Compile Java
RUN cd Backend && javac -d . src/com/ems/*.java

EXPOSE 8080

CMD ["sh", "-c", "cd Backend && java com.ems.Main"]