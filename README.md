# Yandex Practicum Middle Java Developer — Модуль 3

Микросервисное приложение «Банк» — веб-приложение для управления виртуальными счетами.

## Функциональность

- Редактирование данных аккаунта (ФИО, дата рождения)
- Пополнение и снятие денег со счёта
- Перевод денег между аккаунтами
- Получение уведомлений об операциях

---

## Архитектура

### Сервисы

| Сервис | Порт | Описание |
|--------|------|----------|
| **frontend** | 8081 | Веб-интерфейс (Thymeleaf + Spring Security OAuth2) |
| **gateway** | 8086 | API Gateway (единая точка входа) |
| **accounts** | 8082 | Сервис аккаунтов пользователей |
| **cash** | 8084 | Сервис операций с наличными |
| **transfer** | 8085 | Сервис переводов между аккаунтами |
| **notifications** | 8083 | Сервис уведомлений |

### Инфраструктура

| Компонент | Порт | Описание |
|-----------|------|----------|
| **Consul** | 8500 | Service Discovery |
| **Keycloak** | 8180 | OAuth2/OIDC провайдер |
| **PostgreSQL** | 5432 | Основная БД (schema per service) |

### Схема взаимодействия

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Browser   │ ──► │  Frontend   │ ──► │   Gateway   │
│ (port 8081) │     │ (port 8081) │     │ (port 8086) │
└─────────────┘     └─────────────┘     └──────┬──────┘
                                               │
         ┌─────────────────────────────────────┼─────────────────────────────────────┐
         │                                     │                                     │
         ▼                                     ▼                                     ▼
┌─────────────────┐              ┌─────────────────┐              ┌─────────────────┐
│    Accounts     │              │      Cash       │              │    Transfer     │
│   (port 8082)   │              │   (port 8084)   │              │   (port 8085)   │
│  + PostgreSQL   │              │  + PostgreSQL   │              │  + PostgreSQL   │
│  + Outbox       │              │                 │              │                 │
└────────┬────────┘              └────────┬────────┘              └────────┬────────┘
         │                                │                                │
         └────────────────────────────────┼────────────────────────────────┘
                                          │
                                          ▼
                                 ┌─────────────────┐
                                 │  Notifications  │
                                 │   (port 8083)   │
                                 │  + PostgreSQL   │
                                 └─────────────────┘
```

**Паттерн Transactional Outbox:**
- **accounts** → outbox_messages → notifications (уведомления о создании/изменении аккаунта)

**Прямая отправка сообщений:**
- **cash** → notifications (уведомления о пополнении/снятии)
- **transfer** → notifications (уведомления о переводах)

---

## Технологический стек

- **Java 21**
- **Spring Boot 4.0.6** (сервисы), **3.4.4** (gateway)
- **Spring Cloud 2025.1.0** (сервисы), **2025.0.0** (gateway)
- **Spring Web MVC** (сервисы), **Spring WebFlux** (gateway)
- **Spring Data JPA** (PostgreSQL)
- **Spring Security OAuth2** (Keycloak)
- **Spring Cloud Gateway**
- **Consul** (Service Discovery)
- **Resilience4j** (Circuit Breaker)
- **Spring Cloud Contract** (Contract Testing)
- **OpenAPI 3.0** (Swagger)
- **Lombok**
- **Docker & Docker Compose**

---

## Быстрый старт

### Предварительные требования

- Docker & Docker Compose
- Java 21

### Запуск всех сервисов

```bash
make up-local-infra
# или
docker-compose up --force-recreate --renew-anon-volumes -d
```

### Остановка всех сервисов

```bash
make down-local-infra
# или
docker-compose down --remove-orphans -v
```

### Проверка статуса

```bash
docker-compose ps
```

---

## Разработка

### Сборка всех модулей

```bash
./gradlew build
```

### Запуск отдельных сервисов (локально)

```bash
# Accounts service
./gradlew :accounts:bootRun

# Cash service
./gradlew :cash:bootRun

# Transfer service
./gradlew :transfer:bootRun

# Notifications service
./gradlew :notifications:bootRun

# Gateway service
./gradlew :gateway:bootRun

# Frontend service
./gradlew :frontend:bootRun
```

---

## Тестирование

### Запуск всех тестов

```bash
./gradlew test contractTest
```

### Покрытие тестами

| Сервис | Unit | Integration | Contract |
|--------|------|-------------|----------|
| **accounts** | ✓ | ✓ | ✓ |
| **cash** | ✓ | ✓ | ✓ |
| **transfer** | ✓ | ✓ | ✓ |
| **notifications** | ✓ | ✓ | ✓ |
| **gateway** | ✓ | ✓ | — |
| **frontend** | ✓ | ✓ | — |

### Интеграционные тесты

**notifications:**
- `NotificationServiceIntegrationTest` — тесты сервиса с PostgreSQL
- `NotificationControllerIntegrationTest` — тесты контроллера без аутентификации
- `NotificationControllerKeycloakIntegrationTest` — тесты с JWT токенами от Keycloak

**accounts:**
- `OutboxServiceIntegrationTest` — тесты outbox паттерна
- `OutboxProcessorIntegrationTest` — тесты обработки outbox сообщений
- `OutboxSchedulerIntegrationTest` — тесты планировщика

**cash:**
- `CashServiceIntegrationTest` — тесты сервиса с PostgreSQL (транзакции, поиск по статусу/логину)

**transfer:**
- `TransferServiceIntegrationTest` — тесты сервиса с PostgreSQL (переводы, поиск по отправителю/получателю)

**gateway:**
- `GatewayRoutesIntegrationTest` — тесты маршрутизации
- `GatewaySecurityIntegrationTest` — тесты безопасности

### Контрактные тесты

Контракты расположены в `src/contractTest/resources/contracts/`:

```bash
# Accounts contracts (7 контрактов)
accounts/src/contractTest/resources/contracts/

# Cash contracts (2 контракта)
cash/src/contractTest/resources/contracts/

# Transfer contracts (1 контракт)
transfer/src/contractTest/resources/contracts/

# Notifications contracts (1 контракт)
notifications/src/contractTest/resources/contracts/
```

---

## OAuth2 Аутентификация

### Authorization Code Flow (Frontend)

Пользователь аутентифицируется через Keycloak:

```
Browser → Frontend (/login) → Keycloak → Frontend (JWT в сессии)
```

**Конфигурация:**
- Client ID: `frontend-client`
- Client Secret: `frontend-secret`
- Redirect URI: `http://localhost:8081/login/oauth2/code/frontend-client`
- Scopes: `openid, profile, email`

### Client Credentials Flow (Межсервисное взаимодействие)

Сервисы аутентифицируются друг перед другом:

```
Frontend → Gateway → Accounts (с JWT токеном)
```

**Конфигурация сервисов:**

| Сервис | Client ID | Client Secret |
|--------|-----------|---------------|
| accounts | `accounts-client` | `accounts-secret` |
| cash | `cash-client` | `cash-secret` |
| transfer | `transfer-client` | `transfer-secret` |
| gateway | `gateway-client` | `gateway-secret` |

### Тестовые пользователи Keycloak

| Username | Password | Roles |
|----------|----------|-------|
| `user` | `password` | `user` |
| `admin` | `admin` | `admin, user` |

---

## API Документация

### OpenAPI спецификация

Сервис **gateway** предоставляет OpenAPI документацию:

```bash
# Генерация OpenAPI docs
./gradlew :gateway:generateOpenApiDocs

# Путь к файлу
gateway/build/openapi.json
```

### Swagger UI

После запуска gateway сервис доступен по адресу:

```
http://localhost:8086/swagger-ui.html
```

### Основные эндпоинты

| Метод | Путь | Описание |
|-------|------|----------|
| `GET` | `/gateway/account` | Получить данные аккаунта |
| `PUT` | `/gateway/account` | Обновить данные аккаунта |
| `POST` | `/gateway/cash` | Пополнить/снять деньги |
| `POST` | `/gateway/transfer` | Перевод на другой аккаунт |
| `GET` | `/gateway/accounts` | Список аккаунтов для перевода |

---

## База данных

PostgreSQL инициализируется схемой и тестовыми данными:

| Файл | Описание |
|------|----------|
| `scripts/schema.sql` | Схема базы данных |
| `scripts/data.sql` | Тестовые данные |

### Структура БД

```sql
accounts.accounts          -- Аккаунты пользователей
accounts.outbox_messages   -- Outbox для событий (Transactional Outbox pattern)
cash.cash_transactions     -- Транзакции наличных
transfer.transfers         -- Переводы между аккаунтами
notifications.notifications-- Уведомления
```

---

## Проверка работоспособности

### Health Check эндпоинты

| Сервис | URL |
|--------|-----|
| Frontend | `http://localhost:8081/actuator/health` |
| Gateway | `http://localhost:8086/actuator/health` |
| Accounts | `http://localhost:8082/actuator/health` |
| Cash | `http://localhost:8084/actuator/health` |
| Transfer | `http://localhost:8085/actuator/health` |
| Notifications | `http://localhost:8083/actuator/health` |

### Consul UI

Service Discovery доступен по адресу:

```
http://localhost:8500
```

### Keycloak Admin Console

```
http://localhost:8180
Логин: admin
Пароль: admin
Realm: bank
```

---

## Docker

### Сборка образов

```bash
docker-compose build
```

### Просмотр логов

```bash
# Все сервисы
docker-compose logs -f

# Конкретный сервис
docker-compose logs -f accounts
```

### Пересборка конкретного сервиса

```bash
docker-compose build accounts
docker-compose up -d accounts
```

---

## Makefile команды

| Команда | Описание |
|---------|----------|
| `make up-local-infra` | Запуск всех сервисов |
| `make down-local-infra` | Остановка всех сервисов |
| `make build` | Сборка всех модулей |
| `make test` | Запуск тестов |

---

## Troubleshooting

### Ошибка: "No gateway instances found in Consul"

**Причина:** Gateway сервис не зарегистрировался в Consul.

**Решение:**
```bash
docker-compose logs gateway
docker-compose restart gateway
```

### Ошибка: "JWT token is null"

**Причина:** Пользователь не аутентифицирован.

**Решение:** Перейти на `/login` и войти в систему.

### Ошибка: "Connection refused to PostgreSQL"

**Причина:** PostgreSQL контейнер не запущен.

**Решение:**
```bash
docker-compose ps postgres
docker-compose up -d postgres
```

### Ошибка: "Keycloak realm not found"

**Причина:** Realm не импортирован при старте.

**Решение:**
```bash
docker-compose down -v
docker-compose up --force-recreate -d
```

### Ошибка: "CircuitBreaker has exceeded failure rate threshold"

**Причина:** Сервис-получатель недоступен или отвечает слишком медленно.

**Решение:**
```bash
# Проверить статус сервиса
docker-compose ps <service-name>

# Проверить логи сервиса
docker-compose logs -f <service-name>

# Перезапустить сервис
docker-compose restart <service-name>

# Если проблема в сети — проверить Consul
curl http://localhost:8500/v1/catalog/services
```

### Ошибка: "CircuitBreaker is OPEN"

**Причина:** CircuitBreaker открыт после серии неудачных запросов.

**Решение:**
```bash
# Подождать автоматического закрытия (по умолчанию 30 сек)
# Или перезапустить gateway
docker-compose restart gateway

# Проверить метрики CircuitBreaker
curl http://localhost:8086/actuator/circuitbreakerevents
curl http://localhost:8086/actuator/circuitbreakerevents/<circuit-breaker-name>
```

### Ошибка: "No servers available for service: accounts-service"

**Причина:** Сервис не зарегистрирован в Consul Discovery.

**Решение:**
```bash
# Проверить регистрацию в Consul
curl http://localhost:8500/v1/catalog/service/accounts-service

# Проверить логи сервиса
docker-compose logs accounts

# Убедиться, что SPRING_CLOUD_CONSUL_HOST настроен правильно
docker-compose exec accounts env | grep CONSUL

# Перезапустить сервис
docker-compose restart accounts
```

### Ошибка: "Consul health check failed"

**Причина:** Health check сервиса не проходит.

**Решение:**
```bash
# Проверить health check в Consul
curl http://localhost:8500/v1/health/checks/accounts-service

# Проверить actuator health
curl http://localhost:8082/actuator/health

# Перезапустить сервис
docker-compose restart accounts
```

---

## Безопасность

> ⚠️ **Внимание:** Текущая конфигурация Keycloak предназначена **ТОЛЬКО ДЛЯ РАЗРАБОТКИ**.
>
> В production необходимо:
> - Использовать `start` вместо `start-dev`
> - Настроить HTTPS
> - Использовать сложные пароли
> - Отключить дефолтные учётные данные
> - Установить `KC_HOSTNAME_STRICT_HTTPS=true`
> - Использовать внешнюю базу данных (PostgreSQL) вместо `dev-file`
> - Настроить CORS для конкретных доменов
> - Включить audit logging событий аутентификации

---

## Лицензия

Учебный проект Yandex Practicum.
