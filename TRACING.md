# Распределённая трассировка с Zipkin

## Обзор

В проекте реализована система распределённой трассировки на базе **Zipkin** и **Micrometer Tracing**.

Каждый микросервис отправляет трейсы в Zipkin для отслеживания запросов across сервисов.

---

## Архитектура

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Browser   │ ──► │  Frontend   │ ──► │   Gateway   │
│             │     │             │     │             │
└─────────────┘     └──────┬──────┘     └──────┬──────┘
                           │                   │
                           │                   │
                           ▼                   ▼
                    ┌─────────────┐     ┌─────────────┐
                    │   Accounts  │     │    Cash     │
                    │   Service   │     │   Service   │
                    └──────┬──────┘     └──────┬──────┘
                           │                   │
                           └────────┬──────────┘
                                    │
                                    ▼
                           ┌─────────────────┐
                           │     Zipkin      │
                           │   (Tracing)     │
                           └─────────────────┘
```

---

## Компоненты

### Zipkin Server

**Helm-чарт:** `helm/zipkin/`

| Параметр | Значение |
|----------|----------|
| Image | `openzipkin/zipkin:2.24.3` |
| Port | 9411 |
| Storage | In-memory (dev) / Elasticsearch (prod) |
| UI | `http://localhost:9411` |

**Конфигурация:**
- `STORAGE_TYPE=mem` — хранение в памяти (dev)
- `JAVA_OPTS=-Xmx1024m` — память для Zipkin

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

- **Генерирует** `trace_id` для каждого входящего запроса от браузера
- Передаёт `trace_id` и `span_id` в заголовках HTTP-запросов к Gateway
- Заголовки: `X-B3-TraceId`, `X-B3-SpanId`, `X-B3-ParentSpanId`

### Gateway

- Принимает `trace_id` от Frontend
- Генерирует дочерние `span_id` для каждого маршрута
- Передаёт трейсы в Zipkin
- Пробрасывает `trace_id` и новый `span_id` в downstream-сервисы (accounts, cash, transfer)

### Backend-сервисы (accounts, cash, transfer, notifications)

- Принимают `trace_id` и `parent_span_id` из HTTP-заголовков
- Генерируют собственный `span_id`
- Отправляют трейсы в Zipkin:
  - **HTTP-запросы** (входящие/исходящие)
  - **Запросы к БД** (JPA/Hibernate)
  - **Kafka-сообщения** (producer/consumer)

### Kafka Tracing

Трейсы запросов в Kafka автоматически инструментизируются:

- **Producer:** создаёт дочерний span для каждого сообщения
- **Consumer:** извлекает `trace_id` из заголовков Kafka-сообщения

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
# helm/<service>/values.yaml
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

1. **По сервису:** Выберите сервис из dropdown (frontend, gateway, accounts, etc.)
2. **По trace ID:** Вставьте `trace_id` из логов
3. **По длительности:** Найдите медленные запросы

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
│     │  └─ Span 4: PostgreSQL (SELECT) — 25ms
│     │
│     └─ Span 5: Kafka (send notification) — 15ms
│        │
│        └─ Span 6: notifications (consume event) — 10ms
│           │
│           └─ Span 7: PostgreSQL (INSERT) — 5ms
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

1. Проверьте доступность Zipkin:
   ```bash
   kubectl get pods -l app=zipkin
   kubectl logs -l app=zipkin
   ```

2. Проверьте переменные окружения в сервисах:
   ```bash
   kubectl exec -it <pod-name> -- env | grep TRACING
   ```

3. Проверьте логи на ошибки отправки:
   ```bash
   kubectl logs -l app=accounts | grep -i zipkin
   ```

### Неполные трейсы

- Убедитесь, что все сервисы передают заголовки трассировки
- Проверьте `sampling.probability` (должен быть > 0)
- Убедитесь, что Zipkin доступен из всех namespace'ов

### Zipkin UI не открывается

```bash
# Проверьте сервис
kubectl get svc zipkin

# Проверьте port-forward
kubectl port-forward svc/zipkin 9411:9411 -n bank-dev
```

---

## Метрики

### Actuator Endpoints

```bash
# Проверка метрик трассировки
curl http://localhost:8080/actuator/metrics/tracing
```

### Prometheus Metrics

```
micrometer_tracing_spans_created_total
micrometer_tracing_spans_sampled_total
micrometer_tracing_spans_dropped_total
```

---

## Ссылки

- [Micrometer Tracing Docs](https://docs.micrometer.io/tracing/reference/)
- [Zipkin Documentation](https://zipkin.io/pages/quickstart.html)
- [Brave GitHub](https://github.com/openzipkin/brave)
