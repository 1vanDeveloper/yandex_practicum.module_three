# Kibana преднастройка логов

## Автоматическая настройка

Kibana автоматически настраивается при деплое через Helm Job:
- **Data View:** `bank-logs-*` (Logstash пишет в `bank-logs-%{+YYYY.MM.dd}`)
- **Saved Searches:** All Logs, Errors Only, Logs with Trace ID, по одному на сервис
- **Job:** Запускается после деплоя Kibana, создаёт объекты через Saved Objects API

При успешной настройке Job автоматически удаляется (hook-delete-policy: hook-succeeded).

## Доступ
- **URL:** http://localhost:30561
- **Логин:** admin
- **Пароль:** admin

## Data View (автоматически создаётся)

| Name | Index Pattern | Time Field | Описание |
|------|---------------|------------|----------|
| Bank Logs | `bank-logs-*` | @timestamp | Основной Data View для логов приложений |

## Saved Searches

### Общие
1. **All Logs** - Все логи сервисов, сортировка по времени
   - Колонки: service, level, traceId, spanId, logger_name, message
   - Query: (пусто - все логи)

2. **Errors Only** - Только ошибки и предупреждения
   - Колонки: service, level, traceId, spanId, message
   - Query: `level: ERROR or level: WARN`

3. **Logs with Trace ID** - Логи с traceId для трейсинга
   - Колонки: service, level, traceId, spanId, message
   - Query: `traceId: *`

### По сервисам
4. **Logs: accounts** - Логи сервиса accounts
5. **Logs: cash** - Логи сервиса cash
6. **Logs: transfer** - Логи сервиса transfer
7. **Logs: notifications** - Логи сервиса notifications
8. **Logs: gateway** - Логи сервиса gateway
9. **Logs: frontend** - Логи сервиса frontend

## Dashboard
- **Name:** Bank Logs Dashboard
- **ID:** 7adda550-8093-11f1-b065-0f8170cd9d4c
- **Описание:** Centralized logging dashboard for all microservices with trace correlation

## Доступные поля для фильтрации

| Поле | Описание | Пример |
|------|----------|--------|
| @timestamp | Время лога | 2026-07-15T21:23:41.387Z |
| service | Название сервиса | accounts, cash, gateway |
| level | Уровень лога | DEBUG, INFO, WARN, ERROR |
| traceId | ID трейса | 6a57fa0fd685017f... |
| spanId | ID спана | af89b3dd964b5eab |
| logger_name | Логгер | org.hibernate.SQL |
| thread_name | Поток | http-nio-8080-exec-1 |
| message | Сообщение лога | select om1_0.id... |

## Примеры KQL запросов

```
# Логи конкретного сервиса
service: accounts

# Ошибки в сервисе
service: gateway and level: ERROR

# Логи с конкретным traceId
traceId: 6a57fa0fd685017f214dda2c9eeba17c

# SQL запросы
logger_name: org.hibernate.SQL

# Логи за последние 5 минут
@timestamp > now-5m

# Комбинированный запрос
service: cash and level: ERROR and traceId: *
```

## Корреляция с Zipkin

1. Найти лог в Kibana с traceId
2. Скопировать traceId
3. Открыть Zipkin: http://localhost:9411/zipkin/
4. Вставить traceId в поиск
5. Просмотреть полный трейс across services

## Интеграция с Grafana

Для кросс-платформенного мониторинга:
- Grafana: http://localhost:30030 (метрики + алерты)
- Kibana: http://localhost:30561 (логи + трейсинг)
- Zipkin: http://localhost:9411/zipkin/ (трейсы)

## Troubleshooting

### Проверка статуса инициализации
```bash
# Статус Job
kubectl get job kibana-init

# Логи инициализации
kubectl logs -l app=kibana-init

# Если Job не запустился — проверить под Kibana
kubectl get pods -l app=kibana
kubectl logs -l app=kibana
```

### Ручной запуск инициализации
```bash
# Удалить старый Job
kubectl delete job kibana-init

# Запустить Helm upgrade для пересоздания Job
helm upgrade --install bank helm/bank -f helm/values-dev.yaml -f helm/values-secret.yaml
```

### Проверка Data Views через API
```bash
# Port-forward Kibana
kubectl port-forward svc/kibana 5601:5601 &

# Список Data Views
curl http://localhost:5601/api/saved_objects/_find?type=data-view

# Список Saved Searches
curl http://localhost:5601/api/saved_objects/_find?type=search
```
