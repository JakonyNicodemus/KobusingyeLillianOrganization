# Use a small, fast Java 17 image
FROM eclipse-temurin:17-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy your entire project into the container
COPY . /app

# Compile the Java code
RUN javac -d . Backend/src/com/ems/*.java

# Expose the port your application uses
EXPOSE 8080

# Command to start your application
CMD ["java", "com.ems.Main"]