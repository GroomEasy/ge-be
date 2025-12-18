# --- Build Stage ---
FROM gradle:8.7-jdk21 AS builder
WORKDIR /app

COPY . .

# gradle wrapper 권한 주고 빌드
RUN chmod +x ./gradlew && ./gradlew clean build -x test


# --- Run Stage ---
FROM eclipse-temurin:21-jdk
WORKDIR /app

COPY --from=builder /app/build/libs/*SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]
