FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY . .

RUN mkdir -p out && javac -d out src/com/ems/*.java

EXPOSE 8080

CMD ["java", "-cp", "out", "com.ems.Main"]