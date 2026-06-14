# Yandex Practicum Middle Java Developer — Модуль 3

## Функциональность

Микросервисное приложение «Банк» — это приложение с веб-интерфейсом (фронт), которое позволяет пользователю (клиенту банка):

- редактировать данные своего аккаунта (фамилию и имя, дату рождения);
- класть виртуальные деньги на счёт своего аккаунта и снимать их;
- переводить виртуальные деньги на счёт другого аккаунта.

Приложение состоит из следующих частей:

- **frontend** — сервис UI (Thymeleaf + Spring Security OAuth2)
- **gateway** — API Gateway для маршрутизации запросов
- **accounts** — сервис аккаунтов пользователей
- **cash** — сервис обналичивания денег
- **transfer** — сервис перевода денег между аккаунтами
- **notifications** — сервис уведомлений

---

## Технологический стек

- Java 21
- Spring Boot 3.4.4
- Spring WebFlux (reactive)
- Spring Cloud 2024.0.0
- Spring Data JPA (PostgreSQL)
- Spring Security OAuth2 (Keycloak)
- OpenAPI 3.0
- Lombok
- Docker & Docker Compose
- Consul (Service Discovery)
- Spring Cloud Gateway
- **Resilience4j** (Circuit Breaker, Retry, Rate Limiter)

---

## Архитектура

### Схема взаимодействия сервисов

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
└─────────────────┘              └─────────────────┘              └─────────────────┘
         │
         ▼
┌─────────────────┐
│  Notifications  │
│   (port 8083)   │
│  + PostgreSQL   │
└─────────────────┘
```

### Паттерны микросервисов

| Паттерн | Реализация |
|---------|------------|
| **API Gateway** | Spring Cloud Gateway (`gateway/`) |
| **Service Discovery** | Consul (`spring-cloud-starter-consul-discovery`) |
| **OAuth2 Authorization Code Flow** | Frontend → Keycloak → Gateway |
| **OAuth2 Client Credentials** | Межсервисное взаимодействие |
| **Transactional Outbox** | `accounts/service/OutboxService.java`, `accounts/service/OutboxScheduler.java` |
| **Circuit Breaker** | ✅ Resilience4j (`spring-cloud-starter-circuitbreaker-resilience4j`) |
| **Contract Testing** | Spring Cloud Contract (accounts, cash, transfer, notifications) |

---

## Порты сервисов

| Сервис | Порт (хост:контейнер) | Описание |
|--------|----------------------|----------|
| **frontend** | 8081:8080 | Веб-интерфейс (Thymeleaf) |
| **gateway** | 8086:8080 | API Gateway (единая точка входа) |
| **accounts** | 8082:8080 | Сервис аккаунтов |
| **cash** | 8084:8080 | Сервис операций с наличными |
| **transfer** | 8085:8080 | Сервис переводов |
| **notifications** | 8083:8080 | Сервис уведомлений |
| **consul** | 8500:8500 | Service Discovery UI |
| **keycloak** | 8180:8080 | OAuth2 провайдер |
| **postgres** | 5432:5432 | Основная БД |

---

## Быстрый старт

### Предварительные требования

- Docker & Docker Compose
- Java 21
- Gradle 8.x

### Запуск в Docker Compose (рекомендуется)

Запуск всех сервисов:

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

Проверка статуса:

```bash
docker-compose ps
```

### Локальная разработка

#### Сборка всех модулей

```bash
./gradlew build
```

#### Запуск отдельных сервисов

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

## OAuth2 Аутентификация

### Authorization Code Flow (Frontend)

Пользователь аутентифицируется через Keycloak:

```
Browser → Frontend (/login) → Keycloak → Frontend (JWT в сессии)
```

**Конфигурация:**
- Client ID: `frontend-client`
- Redirect URI: `http://localhost:8081/login/oauth2/code/frontend-client`
- Scopes: `openid, profile, email`

### Client Credentials Flow (Межсервисное взаимодействие)

Сервисы аутентифицируются друг перед другом:

```
Frontend → Gateway → Accounts (с JWT токеном)
```

**Конфигурация сервисов:**

| Сервис | Client ID |
|--------|-----------|
| accounts | `accounts-service` |
| cash | `cash-service` |
| transfer | `transfer-service` |
| gateway | `gateway-service` |

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
accounts.outbox_messages   -- Outbox для событий
cash.cash_transactions     -- Транзакции наличных
transfer.transfers         -- Переводы между аккаунтами
notifications.notifications-- Уведомления
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
| accounts | ✓ | ✓ | ✓ (7) |
| cash | ✓ | ✓ | ✓ (2) |
| transfer | ✓ | ✓ | ✓ (1) |
| notifications | ✓ | ✗ | ✓ (1) |
| gateway | ✓ | ✓ | ✗ |
| frontend | ✓ | ✓ | ✗ |

**Gateway тесты:**
- `GatewayApplicationTest` — загрузка контекста приложения
- `GatewayRoutesConfigTest` — конфигурация маршрутов
- `GatewayRoutesIntegrationTest` — integration-тесты маршрутизации
- `GatewaySecurityIntegrationTest` — integration-тесты безопасности
- `JwtAuthFilterTest` — unit-тесты фильтра JWT

**Контрактные тесты:**
- **accounts**: getMyAccount, updateMyAccount, getAccount, internalDebit, internalWithdraw, internalCredit, internalDeposit
- **cash**: processCashDeposit, processCashWithdraw
- **transfer**: createTransfer
- **notifications**: notificate

### Контрактные тесты

Контракты расположены в `src/contractTest/resources/contracts/`:

```bash
# Accounts contracts
accounts/src/contractTest/resources/contracts/

# Cash contracts
cash/src/contractTest/resources/contracts/
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
```

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

---

## Makefile команды

| Команда | Описание |
|---------|----------|
| `make up-local-infra` | Запуск всех сервисов |
| `make down-local-infra` | Остановка всех сервисов |
| `make logs` | Просмотр логов |
| `make build` | Сборка всех модулей |
| `make test` | Запуск тестов |

---

## Безопасность

> ⚠️ **Внимание:** Текущая конфигурация Keycloak предназначена **ТОЛЬКО ДЛЯ РАЗРАБОТКИ**.
>
> В production необходимо:
> - Использовать `start` вместо `start-dev`
> - Настроить HTTPS
> - Использовать сложные пароли
> - Отключить дефолтные учётные данные
> - Установить `KC_HOSTNAME_STRICT_HTTPS=true` (в `docker-compose.yml` сейчас `false` для dev)
> - Использовать внешнюю базу данных (PostgreSQL/H2) вместо `dev-file`
> - Настроить CORS для конкретных доменов
> - Включить audit logging событий аутентификации

---

## Лицензия

Учебный проект Yandex Practicum.
