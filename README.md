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
| **frontend** | 8080 | Веб-интерфейс (Thymeleaf + Spring Security OAuth2) |
| **gateway** | 8080 | API Gateway (единая точка входа) |
| **accounts** | 8080 | Сервис аккаунтов пользователей |
| **cash** | 8080 | Сервис операций с наличными |
| **transfer** | 8080 | Сервис переводов между аккаунтами |
| **notifications** | 8080 | Сервис уведомлений |

### Инфраструктура

| Компонент | Порт | Описание |
|-----------|------|----------|
| **Keycloak** | 8080 | OAuth2/OIDC провайдер |
| **PostgreSQL** | 5432 | Основная БД (schema per service) |

### Схема взаимодействия

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Browser   │ ──► │  Frontend   │ ──► │   Gateway   │
│  (NodePort) │     │  (ClusterIP)│     │ (ClusterIP) │
└─────────────┘     └─────────────┘     └──────┬──────┘
                                               │
         ┌─────────────────────────────────────┼─────────────────────────────────────┐
         │                                     │                                     │
         ▼                                     ▼                                     ▼
┌─────────────────┐              ┌─────────────────┐              ┌─────────────────┐
│    Accounts     │              │      Cash       │              │    Transfer     │
│   (ClusterIP)   │              │  (ClusterIP)    │              │  (ClusterIP)    │
│  + PostgreSQL   │              │  + PostgreSQL   │              │  + PostgreSQL   │
│  + Outbox       │              │                 │              │                 │
└────────┬────────┘              └────────┬────────┘              └────────┬────────┘
         │                                │                                │
         └────────────────────────────────┼────────────────────────────────┘
                                          │
                                          ▼
                                 ┌─────────────────┐
                                 │  Notifications  │
                                 │  (ClusterIP)    │
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
- **Kubernetes** (Service Discovery через DNS)
- **Resilience4j** (Circuit Breaker)
- **Spring Cloud Contract** (Contract Testing)
- **OpenAPI 3.0** (Swagger)
- **Lombok**
- **Docker, Colima & Helm**

---

## Быстрый старт

### Предварительные требования

- Java 21+
- Docker
- Colima + Kubernetes
- kubectl
- Helm 3.x
- Git

---

## Локальная разработка

### 1. Клонирование репозитория

```bash
git clone https://github.com/1vanDeveloper/yandex_practicum.module_three.git
cd yandex_practicum.module_three
```

### 2. Сборка проекта

```bash
./gradlew build
```

### 3. Запуск Colima с Kubernetes

```bash
# Запуск Colima с Kubernetes (если не запущен)
colima start --kubernetes

# Проверка
colima status
kubectl cluster-info
kubectl get nodes
```

### 4. Развёртывание приложения

```bash
# Сборка Docker образов (автоматически доступны в Colima)
make docker-build

# Развёртывание через Helm
make k8s-deploy

# Или вручную:
# helm upgrade --install bank helm/bank --timeout 5m --wait
```

### 5. Проверка статуса

```bash
# Проверка подов
kubectl get pods -l app.kubernetes.io/part-of=bank

# Проверка сервисов
kubectl get svc

# Проверка логов
kubectl logs -l app=accounts -f
```

### 6. Доступ к приложению

```bash
# Frontend доступен через NodePort 32190
open http://localhost:32190

# Keycloak Admin Console
open http://localhost:8180
# Логин: admin / Пароль: admin
# Realm: bank
```

### 7. Port-forward для отладки

```bash
# PostgreSQL для интеграционных тестов
kubectl port-forward svc/postgresql 5432:5432 &

# Keycloak для интеграционных тестов
kubectl port-forward svc/keycloak 8180:8080 &

# Frontend
kubectl port-forward svc/frontend 8080:8080
```

---

## Тестирование

### Все тесты

```bash
make test
# или
./gradlew test contractTest
```

### Unit-тесты

```bash
./gradlew test
```

### Интеграционные тесты

**Требования:** Запущены PostgreSQL и Keycloak через port-forward

```bash
# Настроить port-forward
kubectl port-forward svc/postgresql 5432:5432 &
kubectl port-forward svc/keycloak 8180:8080 &

# Accounts
./gradlew :accounts:test --tests "*IntegrationTest*"

# Cash
./gradlew :cash:test --tests "*IntegrationTest*"

# Transfer
./gradlew :transfer:test --tests "*IntegrationTest*"

# Notifications
./gradlew :notifications:test --tests "*IntegrationTest*"

# Gateway
./gradlew :gateway:test --tests "*IntegrationTest*"
```

### Контрактные тесты

```bash
./gradlew contractTest
```

### Helm-тесты

**Требования:** Установлен плагин helm-unittest

```bash
# Установка плагина
helm plugin install https://github.com/helm-unittest/helm-unittest.git --verify=false

# Запуск тестов
make helm-test

# Только lint
make helm-lint

# Только unit-тесты
make helm-unit-test
```

---

## Развёртывание в Kubernetes

### Предварительные требования

- Colima с Kubernetes запущена
- kubectl настроен на кластер
- Helm 3.x установлен

### 1. Подготовка кластера

```bash
# Запуск Colima с Kubernetes
colima start --kubernetes

# Проверка
kubectl cluster-info
```

### 2. Установка Helm-чартов

```bash
# Установка приложения
make k8s-deploy

# Или вручную:
helm upgrade --install bank helm/bank --timeout 5m --wait
```

### 3. Проверка статуса

```bash
# Проверка подов
kubectl get pods -l app.kubernetes.io/part-of=bank

# Проверка сервисов
kubectl get svc

# Проверка логов
kubectl logs -l app=frontend -f
kubectl logs -l app=gateway -f
```

### 4. Доступ к приложению

```bash
# Frontend доступен через NodePort 32190
open http://localhost:32190

# Или через port-forward
kubectl port-forward svc/frontend 8080:8080
open http://localhost:8080
```

### 5. Управление релизом

```bash
# Просмотр статуса
helm status bank

# История релизов
helm history bank

# Откат к предыдущей версии
make k8s-rollback
# или: helm rollback bank

# Удаление
make k8s-delete
# или: helm uninstall bank
```

---

## Makefile команды

| Команда | Описание |
|---------|----------|
| `make build` | Сборка всех модулей |
| `make docker-build` | Сборка Docker образов (автоматически доступны в Colima) |
| `make k8s-deploy` | Развёртывание в Kubernetes |
| `make k8s-rollback` | Откат релиза |
| `make k8s-status` | Проверка статуса подов и сервисов |
| `make k8s-logs` | Просмотр логов |
| `make k8s-delete` | Удаление релиза |
| `make k8s-port-forward` | Port-forward для отладки |
| `make helm-lint` | Helm lint всех чартов |
| `make helm-unit-test` | Helm unit-тесты |
| `make helm-test` | Helm lint + unit-тесты |
| `make helm-template` | Рендеринг Helm шаблонов |
| `make dev` | Полный цикл: build + docker-build + k8s-deploy |
| `make test` | Запуск всех тестов |

---

## API Документация

### Swagger UI

```bash
# Port-forward для gateway
kubectl port-forward svc/gateway 8086:8080

# Swagger UI доступен по адресу
open http://localhost:8086/swagger-ui.html
```

### Генерация OpenAPI

```bash
./gradlew :gateway:generateOpenApiDocs
# Файл: gateway/build/openapi.json
```

### Основные эндпоинты

| Метод | Путь | Описание |
|-------|------|----------|
| `GET` | `/gateway/account` | Данные аккаунта |
| `PUT` | `/gateway/account` | Обновление аккаунта |
| `POST` | `/gateway/cash` | Пополнить/снять деньги |
| `POST` | `/gateway/transfer` | Перевод между аккаунтами |
| `GET` | `/gateway/accounts` | Список аккаунтов |

---

## База данных

### Структура БД

```sql
-- Schema: accounts
accounts.accounts           -- Аккаунты пользователей
accounts.outbox_messages    -- Outbox для событий

-- Schema: cash
cash.cash_transactions      -- Транзакции наличных

-- Schema: transfer
transfer.transfers          -- Переводы между аккаунтами

-- Schema: notifications
notifications.notifications -- Уведомления
```

### Инициализация

```bash
# Схема и данные создаются автоматически при старте PostgreSQL
scripts/schema.sql   -- Схема БД
scripts/data.sql     -- Тестовые данные
```

---

## OAuth2 Аутентификация

### Пользователи (Authorization Code Flow)

```
Browser → Frontend → Keycloak → Frontend (JWT в сессии)
```

**Конфигурация:**
- Client ID: `frontend-client`
- Client Secret: `frontend-secret`
- Redirect URI: `http://localhost:32190/login/oauth2/code/frontend-client`

### Межсервисное взаимодействие (Client Credentials)

| Сервис | Client ID | Client Secret |
|--------|-----------|---------------|
| accounts | `accounts-client` | `accounts-secret` |
| cash | `cash-client` | `cash-secret` |
| transfer | `transfer-client` | `transfer-secret` |
| gateway | `gateway-client` | `gateway-secret` |

### Тестовые пользователи

| Username | Password | Roles |
|----------|----------|-------|
| `user` | `password` | `user` |
| `admin` | `admin` | `admin, user` |

---

## Troubleshooting

### Ошибка: "ImagePullBackOff"

```bash
# Убедитесь, что Colima запущена с Kubernetes
colima status

# Проверьте статус подов
kubectl get pods

# Пересоздайте поды
kubectl rollout restart deployment accounts cash transfer notifications gateway frontend
```

### Ошибка: "JWT token is null"

Перейти на `/login` и войти в систему через Keycloak.

### Ошибка: "Connection refused to PostgreSQL"

```bash
# Проверить статус PostgreSQL
kubectl get pods -l app=postgresql

# Проверить логи
kubectl logs -l app=postgresql
```

### Ошибка: "CircuitBreaker is OPEN"

```bash
# Подождать 30 сек или перезапустить gateway
kubectl rollout restart deployment gateway

# Проверить метрики
kubectl port-forward svc/gateway 8086:8080
curl http://localhost:8086/actuator/circuitbreakerevents
```

### Ошибка: "No servers available for service: accounts-service"

```bash
# Проверить статус сервиса
kubectl get svc accounts
kubectl get endpoints accounts

# Проверить поды
kubectl get pods -l app=accounts

# Перезапустить сервис
kubectl rollout restart deployment accounts
```

### Ошибка интеграционных тестов: "Connection refused"

```bash
# Запустить port-forward для тестов
kubectl port-forward svc/postgresql 5432:5432 &
kubectl port-forward svc/keycloak 8180:8080 &

# Запустить тесты
./gradlew :cash:test --tests "*IntegrationTest*"
```

### Ошибка: "helm: command not found"

```bash
# Установка Helm
brew install helm  # macOS
# или
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
```

### Ошибка: "colima: command not found"

```bash
# Установка Colima
brew install colima  # macOS

# Запуск с Kubernetes
colima start --kubernetes
```

### Ошибка: "kubectl: command not found"

```bash
# Установка kubectl
brew install kubectl  # macOS
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
> - Использовать внешнюю базу данных вместо `dev-file`

---

## Лицензия

Учебный проект Yandex Practicum.
