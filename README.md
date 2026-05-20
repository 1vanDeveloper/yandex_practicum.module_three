# Yandex Practicum Middle Java Developer — Модуль 2

## Функциональность
Микросервисное приложение «Банк» — это приложение с веб-интерфейсом (фронт), которое позволяет пользователю (клиенту банка):
- редактировать данные своего аккаунта (фамилию и имя, дату рождения);
- класть виртуальные деньги на счёт своего аккаунта и снимать их;
- переводить виртуальные деньги на счёт другого аккаунта.
  Приложение состоит из следующих частей:
- фронт (Front UI);
- микросервис аккаунтов (Accounts);
- микросервис обналичивания денег (Cash);
- микросервис перевода денег на счёт другого аккаунта (Transfer);
- микросервис уведомлений (Notifications).

## Технологический стек

- Java 21
- Spring Boot 3.4.4
- Spring WebFlux (reactive)
- Spring Cloud
- Spring Data R2DBC (PostgreSQL)
- Spring Data Redis (reactive)
- OpenAPI 3.0
- Lombok
- Docker & Docker Compose

## Структура проекта

```
.
├── frontend/        # Сервис UI (порт 8080)
├── accounts/        # Сервис аккаунтов (порт 8081)
├── cash/            # Сервис обналичивания денег (порт 8082)
├── transfer/        # Сервис перевода денег на счёт другого аккаунта (порт 8083)
├── notifications/   # Сервис уведомлений (порт 8084)
├── scripts/         # Скрипты инициализации БД
├── docker-compose.yml
└── Makefile
```

## Быстрый старт

### Запуск в Docker Compose (рекомендуется)

Запуск всех сервисов (приложение, платежи, PostgreSQL, Redis):

```bash
make up-local-infra
# или
docker-compose up --force-recreate --renew-anon-volumes -d
```

Остановка всех сервисов:

```bash
make down-local-infra
# или
docker-compose down --remove-orphans -v
```

### Локальная разработка

#### Сборка всех модулей

```bash
./gradlew build
```

#### Запуск сервиса Market

```bash
./gradlew :market:bootRun
```

Сервис будет доступен по адресу `http://localhost:8080`

#### Запуск сервиса Payments

```bash
./gradlew :payments:bootRun
```

Сервис будет доступен по адресу `http://localhost:8081`

OpenAPI UI: `http://localhost:8081/swagger-ui.html`

## Сборка JAR

Сборка исполняемого JAR для сервиса market:

```bash
./gradlew :market:bootJar
java -jar market/build/libs/market-1.0-SNAPSHOT.jar
```

Сборка исполняемого JAR для сервиса payments:

```bash
./gradlew :payments:bootJar
java -jar payments/build/libs/payments-1.0-SNAPSHOT.jar
```

## Документация API

Сервис payments автоматически генерирует OpenAPI документацию. Сгенерированный `openapi.json` используется сервисом market для создания реактивного WebFlux клиента.

Путь к файлу OpenAPI спецификации:

```
payments/build/openapi.json
```

Для генерации файла выполните команду:

```bash
./gradlew :payments:generateOpenApiDocs
```

## База данных

PostgreSQL инициализируется схемой и тестовыми данными из файлов:
- `scripts/schema.sql` — схема базы данных
- `scripts/data.sql` — тестовые данные

## Проверка работоспособности

- Сервис Market: `http://localhost:8080/actuator/health`
- Сервис Payments: `http://localhost:8081/actuator/health`
