# Multi-service Dockerfile для Java-сервисов банка
# Использование: docker build --build-arg SERVICE_NAME=accounts -t bank-accounts .
# Использование: docker build --build-arg SERVICE_NAME=cash -t bank-cash .

# Этап 1: Сборка (Build stage)
FROM gradle:8.14-jdk21 AS builder

WORKDIR /build

# Копируем файлы сборки для кэширования зависимостей
COPY build.gradle.kts settings.gradle.kts /build/
ARG SERVICE_NAME
COPY ${SERVICE_NAME} /build/${SERVICE_NAME}

# Собираем приложение (пропуская тесты для скорости)
RUN gradle :${SERVICE_NAME}:bootJar --no-daemon -x test

# Этап 2: Запуск (Runtime stage)
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
ARG SERVICE_NAME

# Копируем JAR из этапа сборки (используем wildcard для независимости от версии)
COPY --from=builder /build/${SERVICE_NAME}/build/libs/${SERVICE_NAME}-1.0-SNAPSHOT.jar application.jar

ENTRYPOINT ["java", "-jar", "application.jar"]
