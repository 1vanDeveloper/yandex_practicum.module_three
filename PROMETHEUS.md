# Метрики и мониторинг банковской платформы

## Обзор

В проекте реализована система сбора и анализа метрик на базе **Prometheus** и **Micrometer**.

## Архитектура

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Frontend      │     │   Gateway       │     │   Accounts      │
│   (NodePort)    │     │   (ClusterIP)   │     │   (ClusterIP)   │
│  :8080          │     │  :8080          │     │  :8080          │
│  /actuator/     │     │  /actuator/     │     │  /actuator/     │
│  prometheus     │     │  prometheus     │     │  prometheus     │
└────────┬────────┘     └────────┬────────┘     └────────┬────────┘
         │                       │                       │
         │        ┌──────────────┴──────────────┐        │
         │        │                             │        │
         ▼        ▼                             ▼        ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Prometheus Server                            │
│              (NodePort: 30090, ClusterIP: 9090)                 │
│                   prometheus:9090                               │
└─────────────────────────────────────────────────────────────────┘
         │                       │                       │
         │        ┌──────────────┴──────────────┐        │
         ▼        ▼                             ▼        ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Cash          │     │   Transfer      │     │  Notifications  │
│  :8080          │     │  :8080          │     │  :8080          │
│  /actuator/     │     │  /actuator/     │     │  /actuator/     │
│  prometheus     │     │  prometheus     │     │  prometheus     │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

## Компоненты

### Prometheus Server

**Helm-чарт:** `helm/prometheus/`

| Параметр | Значение |
|----------|----------|
| Image | `prom/prometheus:v2.45.0` |
| Port (ClusterIP) | 9090 |
| Port (NodePort) | 30090 |
| UI | `http://localhost:30090` |
| Retention | 15 дней |
| Storage | 10Gi PersistentVolume |

**Конфигурация scrape:**
- Интервал: 15 секунд
- Path: `/actuator/prometheus`
- Автоматическое обнаружение подов через Kubernetes SD

### Micrometer + Spring Boot Actuator

Все микросервисы используют **Micrometer** с экспортом в **Prometheus**.

**Зависимости (build.gradle.kts):**
```kotlin
implementation("org.springframework.boot:spring-boot-starter-actuator")
runtimeOnly("io.micrometer:micrometer-registry-prometheus")
```

**Конфигурация (application.properties):**
```properties
management.endpoints.web.exposure.include=health,info,prometheus
management.endpoint.prometheus.enabled=true
management.metrics.export.prometheus.enabled=true
management.metrics.tags.application=${spring.application.name}
```

---

## Метрики

### Стандартные метрики (автоматически)

#### HTTP-запросы
- `http.server.requests` — количество, задержки, статусы
- `http.client.requests` — исходящие запросы

#### JVM
- `jvm.memory.used` — использование памяти
- `jvm.memory.max` — максимум памяти
- `jvm.gc.pause` — паузы GC
- `jvm.threads.states` — потоки

#### Process
- `process.cpu.usage` — использование CPU
- `process.files.open` — открытые файлы
- `process.uptime` — время работы

#### Spring Boot
- `spring.data.repository.invocations` — вызовы репозиториев
- `spring.security.http.requests` — безопасность

### Кастомные метрики

#### Accounts Service

| Метрика | Тип | Описание | Теги |
|---------|-----|----------|------|
| `cash_withdrawal_failed_total` | Counter | Неуспешные попытки снятия денег | `login` |
| `transfer_failed_total` | Counter | Неуспешные попытки перевода | `from_login`, `to_login` |

#### Notifications Service

| Метрика | Тип | Описание | Теги |
|---------|-----|----------|------|
| `notification_send_failed_total` | Counter | Неуспешная отправка уведомления | `login` |

---

## Prometheus Alerts

### HTTP ошибки

| Alert | Порог | Severity | Описание |
|-------|-------|----------|----------|
| `HighHttp5xxRate` | >5% ошибок 5xx (5m) | critical | Высокий уровень серверных ошибок |
| `HighHttp4xxRate` | >10% ошибок 4xx (5m) | warning | Высокий уровень клиентских ошибок |

### Задержки

| Alert | Порог | Severity | Описание |
|-------|-------|----------|----------|
| `HighRequestLatencyP95` | p95 > 1с (5m) | warning | Высокая задержка 95-го перцентиля |
| `HighRequestLatencyP99` | p99 > 3с (5m) | critical | Очень высокая задержка 99-го перцентиля |

### Бизнес-метрики

| Alert | Порог | Severity | Описание |
|-------|-------|----------|----------|
| `HighCashWithdrawalFailures` | >0.1/с по login (5m) | warning | Частые неудачи снятия денег |
| `HighTransferFailures` | >0.1/с по паре (5m) | warning | Частые неудачи переводов |
| `HighNotificationFailures` | >0.1/с по login (5m) | warning | Частые неудачи уведомлений |

### Инфраструктура

| Alert | Порог | Severity | Описание |
|-------|-------|----------|----------|
| `HighJvmMemoryUsage` | >90% heap (5m) | warning | Высокое использование памяти JVM |
| `HighCpuUsage` | >80% CPU (5m) | warning | Высокое использование CPU |
| `ServiceDown` | up == 0 (2m) | critical | Сервис недоступен |
| `LowRps` | <0.1 RPS (10m) | warning | Подозрительно низкий трафик |

---

## Доступ к Prometheus

### Port-forward

```bash
kubectl port-forward svc/prometheus-external 9090:9090
```

### Открыть UI

```bash
open http://localhost:9090
```

### Примеры запросов

```promql
# RPS по сервисам
sum(rate(http_server_requests_seconds_count[5m])) by (service)

# Ошибки 5xx
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (service)

# p95 задержка
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, service))

# Использование памяти JVM
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}

# Неуспешные снятия денег по пользователям
sum(rate(cash_withdrawal_failed_total[5m])) by (login)
```

---

## Helm-конфигурация

### Включение Prometheus

```yaml
# helm/values-dev.yaml
prometheus:
  enabled: true
  replicaCount: 1
  externalService:
    enabled: true
    type: NodePort
    nodePort: 30090
  alertingRules:
    enabled: true
```

### Аннотации для scrape

Все сервисы имеют аннотации:
```yaml
metadata:
  annotations:
    prometheus.io/scrape: "true"
    prometheus.io/port: "8080"
    prometheus.io/path: "/actuator/prometheus"
```

---

## Troubleshooting

### Метрики не собираются

1. **Проверьте доступность endpoint:**
   ```bash
   kubectl exec -it <pod-name> -- curl localhost:8080/actuator/prometheus
   ```

2. **Проверьте аннотации пода:**
   ```bash
   kubectl get pod <pod-name> -o yaml | grep prometheus
   ```

3. **Проверьте targets в Prometheus:**
   - Откройте `http://localhost:9090/targets`
   - Убедитесь, что все сервисы в статусе UP

### Alerts не срабатывают

1. **Проверьте правила:**
   ```bash
   kubectl exec -it prometheus-<pod> -- cat /etc/prometheus/rules/banking-alerts.yml
   ```

2. **Проверьте evaluation в Prometheus:**
   - Откройте `http://localhost:9090/rules`

---

## Ссылки

- [Prometheus Documentation](https://prometheus.io/docs/introduction/overview/)
- [Micrometer Docs](https://micrometer.io/docs)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Prometheus Querying](https://prometheus.io/docs/prometheus/latest/querying/basics/)
