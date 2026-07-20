# Распределённая трассировка с Zipkin

## Обзор

В проекте реализована система распределённой трассировки на базе **Zipkin** и **Micrometer Tracing**.

Каждый микросервис отправляет трейсы в Zipkin для отслеживания запросов между сервисами.

---

## Архитектура

### Поток запросов и трассировки

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Browser   │ ──► │  Frontend   │ ──► │   Gateway   │
│             │     │             │     │             │
└─────────────┘     └─────────────┘     └──────┬──────┘
       │                     │                 │
       │ trace               │ trace           │ trace
       ▼                     ▼                 ▼
┌─────────────────────────────────────────────────────────┐
│                      Zipkin Server                      │
│                   http://zipkin:9411                    │
└─────────────────────────────────────────────────────────┘
       ▲                     ▲                 ▲
       │                     │                 │
       │ trace               │ trace           │ trace
┌──────┴──────┐       ┌──────┴──────┐   ┌─────┴─────┐
│  Accounts   │       │    Cash     │   │ Transfer  │
│             │       │             │   │           │
└──────┬──────┘       └──────┬──────┘   └─────┬─────┘
       │                     │                 │
       │                     │                 │
       └─────────────────────┼─────────────────┘
                             │
                             │ Kafka messages
                             │ (с B3-заголовками)
                             ▼
                    ┌─────────────────┐
                    │  Notifications  │
                    │                 │
                    └────────┬────────┘
                             │
                             │ trace
                             ▼
                    ┌─────────────────┐
                    │   Zipkin (cont) │
                    └─────────────────┘
```

**Важно:**
- Каждый сервис отправляет трейсы **напрямую в Zipkin** (HTTP POST на `/api/v2/spans`)
- Kafka **не отправляет** трейсы — это транспорт для сообщений
- B3-заголовки (`X-B3-TraceId`, `X-B3-SpanId`) передаются через Kafka для продолжения трейса

---

## Компоненты

### Zipkin Server

**Helm-чарт:** `helm/zipkin/`

| Параметр | Значение |
|----------|----------|
| Image | `openzipkin/zipkin:2.24.3` |
| Port | 9411 |
| Storage | `mem` (dev), `elasticsearch` (prod) |
| UI | `http://localhost:9411` |
| Health | `/health` |

**Конфигурация (values.yaml):**
```yaml
env:
  STORAGE_TYPE: "mem"
  JAVA_OPTS: "-Xmx1024m -Xms512m"
```

### Micrometer Tracing

Все микросервисы используют **Micrometer Tracing** с мостом **Brave** для совместимости с Zipkin.

**Зависимости (build.gradle.kts):**
```kotlin
implementation("io.micrometer:micrometer-tracing-bridge-brave")
implementation("io.zipkin.reporter2:zipkin-reporter-brave")
```

**Конфигурация (application.properties):**
```properties
management.tracing.enabled=true
management.tracing.sampling.probability=1.0
management.zipkin.tracing.endpoint=http://zipkin:9411/api/v2/spans
```

---

## Трассировка в сервисах

### Frontend

- **Генерирует** `trace_id` для каждого входящего запроса от браузера (автоматически через Micrometer Tracing)
- Передаёт `trace_id` и `span_id` в заголовках HTTP-запросов к Gateway
- Заголовки B3: `X-B3-TraceId`, `X-B3-SpanId`, `X-B3-ParentSpanId`, `X-B3-Sampled`

### Gateway

- Принимает `trace_id` от Frontend
- Генерирует дочерние `span_id` для каждого маршрута
- Передаёт трейсы в Zipkin
- Пробрасывает `trace_id` и новый `span_id` в downstream-сервисы (accounts, cash, transfer)
- **WebFlux-based:** реактивная трассировка через `spring-cloud-starter-circuitbreaker-reactor-resilience4j`

### Backend-сервисы (accounts, cash, transfer, notifications)

- Принимают `trace_id` и `parent_span_id` из HTTP-заголовков
- Генерируют собственный `span_id`
- Отправляют трейсы в Zipkin:
  - **HTTP-запросы** (входящие/исходящие) — автоматически через Spring Web MVC
  - **Запросы к БД** (JPA/Hibernate) — автоматически через Micrometer
  - **Kafka-сообщения** (producer/consumer) — автоматически через Spring Kafka

### Kafka Tracing

Трейсы запросов в Kafka автоматически инструментизируются через Spring Kafka:

- **Producer:** создаёт дочерний span для каждого сообщения, добавляет B3-заголовки в Kafka record
- **Consumer:** извлекает `trace_id` и `span_id` из заголовков Kafka-сообщения, продолжает трейс
- **Топик:** `notifications.events`

---

## Helm-конфигурация

### Глобальные настройки

```yaml
# helm/bank/values.yaml
global:
  zipkin:
    url: "http://zipkin:9411"
```

### Настройки трассировки в сервисах

```yaml
# helm/<service>/values.yaml (accounts, cash, transfer, notifications, gateway, frontend)
tracing:
  enabled: true
  samplingProbability: 1.0  # 1.0 = 100% трейсов, 0.1 = 10%
```

### Включение Zipkin в зонтичный чарт

```yaml
# helm/values-dev.yaml
zipkin:
  enabled: true
  replicaCount: 1
  image:
    repository: "openzipkin/zipkin"
    tag: "2.24.3"
```

---

## Доступ к Zipkin UI

### Port-forward

```bash
kubectl port-forward svc/zipkin 9411:9411 -n bank-dev
```

### Открыть UI

```bash
open http://localhost:9411
```

### Поиск трейсов

1. **По сервису:** выберите сервис из dropdown (frontend, gateway, accounts, cash, transfer, notifications)
2. **По trace ID:** вставьте `trace_id` из логов сервиса
3. **По длительности:** найдите медленные запросы (Sort by Duration)
4. **По имени span:** например, `GET /gateway/account`, `SELECT accounts`, `Kafka send`

---

## Пример трейса

```
Trace ID: abc123def456
│
├─ Span 1: frontend (GET /account) — 150ms
│  │
│  └─ Span 2: gateway (GET /gateway/account) — 120ms
│     │
│     ├─ Span 3: accounts (GET /accounts/me) — 80ms
│     │  │
│     │  └─ Span 4: accounts (SELECT) — 25ms
│     │
│     └─ Span 5: Kafka send (notifications.events) — 15ms
│        │
│        └─ Span 6: notifications (consume) — 10ms
│           │
│           └─ Span 7: notifications (INSERT) — 5ms
```

**B3 заголовки в HTTP-запросах:**
```
X-B3-TraceId: abc123def456
X-B3-SpanId: 789xyz
X-B3-ParentSpanId: 456abc
X-B3-Sampled: 1
```

---

## Настройка для Production

### 1. Хранение трейсов (Elasticsearch)

```yaml
# helm/zipkin/values.yaml
env:
  STORAGE_TYPE: "elasticsearch"
  ES_HOSTS: "elasticsearch:9200"
  ES_INDEX: "zipkin"
```

### 2. Сэмплирование

```yaml
# helm/<service>/values.yaml
tracing:
  samplingProbability: 0.1  # 10% трейсов для production
```

### 3. Ресурсы

```yaml
# helm/zipkin/values.yaml
resources:
  limits:
    cpu: "2000m"
    memory: "2048Mi"
  requests:
    cpu: "1000m"
    memory: "1024Mi"
```

---

## Troubleshooting

### Трейсы не отправляются в Zipkin

1. **Проверьте доступность Zipkin:**
   ```bash
   kubectl get pods -l app=zipkin -n bank-dev
   kubectl logs -l app=zipkin -n bank-dev
   ```

2. **Проверьте переменные окружения в сервисах:**
   ```bash
   kubectl exec -it <pod-name> -n bank-dev -- env | grep -E "TRACING|ZIPKIN"
   ```

3. **Проверьте логи на ошибки отправки:**
   ```bash
   kubectl logs -l app=accounts -n bank-dev | grep -iE "tracing|zipkin|brave"
   ```

4. **Проверьте NetworkPolicy:**
   ```bash
   kubectl get networkpolicy zipkin -n bank-dev
   ```

### Неполные трейсы

- Убедитесь, что все сервисы передают заголовки трассировки (B3 propagation)
- Проверьте `sampling.probability` (должен быть > 0)
- Убедитесь, что Zipkin доступен из всех namespace'ов
- Проверьте логи на наличие ошибок `Connection refused` к Zipkin

### Zipkin UI не открывается

```bash
# Проверьте сервис
kubectl get svc zipkin -n bank-dev

# Проверьте port-forward (завершите старый процесс и запустите новый)
pkill -f "kubectl port-forward"
kubectl port-forward svc/zipkin 9411:9411 -n bank-dev
```

### Трейсы есть в логах, но не в Zipkin

1. Проверьте endpoint Zipkin:
   ```bash
   kubectl exec -it <pod-name> -n bank-dev -- env | grep MANAGEMENT_ZIPKIN
   ```

2. Проверьте доступность endpoint'а из пода:
   ```bash
   kubectl exec -it <pod-name> -n bank-dev -- curl -v http://zipkin:9411/health
   ```

---

## Метрики

### Actuator Endpoints

```bash
# Проверка метрик трассировки
curl http://localhost:8080/actuator/metrics | grep -i tracing
```

### Prometheus Metrics (если включён Prometheus)

```
# Количество созданных span'ов
micrometer_tracing_spans_created_total

# Количество сэмплированных span'ов
micrometer_tracing_spans_sampled_total

# Количество отброшенных span'ов
micrometer_tracing_spans_dropped_total
```

### Логи трассировки

Включите debug-логирование для отладки:
```yaml
# helm/<service>/values.yaml
env:
  LOGGING_LEVEL_IO_MICROMETER: "DEBUG"
  LOGGING_LEVEL_IO_ZIPKIN: "DEBUG"
```

---

## Ссылки

- [Micrometer Tracing Docs](https://docs.micrometer.io/tracing/reference/)
- [Zipkin Documentation](https://zipkin.io/pages/quickstart.html)
- [Brave GitHub](https://github.com/openzipkin/brave)
- [Spring Cloud Gateway Tracing](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-tracing.html)
- [B3 Propagation Format](https://github.com/openzipkin/b3-propagation)
