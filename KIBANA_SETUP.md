# Kibana преднастройка логов

## Автоматическая настройка

Kibana автоматически настраивается при деплое через **Helm Post-Install/Upgrade Job**:
- **Job:** `kibana-init` — запускается после деплоя Kibana
- **Hook:** `post-install,post-upgrade` с `hook-delete-policy: hook-succeeded`
- **Механизм:** curl-запросы к Kibana Saved Objects API

При успешной настройке Job автоматически удаляется.

> ⚠️ **Важно:** InitContainer в deployment.yaml удалён — теперь используется отдельный Helm Job.

## Что создаётся автоматически

### Data View
| Name | Index Pattern | Time Field | Описание |
|------|---------------|------------|----------|
| Bank Logs | `bank-logs-*` | @timestamp | Основной Data View для логов приложений |

### Saved Searches

#### Общие
1. **All Logs** — Все логи сервисов
   - Колонки: `service`, `level`, `traceId`, `spanId`, `logger_name`, `message`
   - Query: (пусто — все логи)
   - Сортировка: по времени (новые сверху)

2. **Errors Only** — Только ошибки и предупреждения
   - Колонки: `service`, `level`, `traceId`, `spanId`, `message`
   - Query: `level: ERROR or level: WARN`
   - Сортировка: по времени

3. **Logs with Trace ID** — Логи с traceId для трейсинга
   - Колонки: `service`, `level`, `traceId`, `spanId`, `message`
   - Query: `traceId: *`
   - Сортировка: по времени

#### По сервисам
4. **Logs: accounts** — Логи сервиса accounts
5. **Logs: cash** — Логи сервиса cash
6. **Logs: transfer** — Логи сервиса transfer
7. **Logs: notifications** — Логи сервиса notifications
8. **Logs: gateway** — Логи сервиса gateway
9. **Logs: frontend** — Логи сервиса frontend

---

## Доступ

- **URL:** http://localhost:30561
- **Логин:** admin (настраивается в values.yaml)
- **Пароль:** admin

---

## Доступные поля для фильтрации

| Поле | Описание | Пример |
|------|----------|--------|
| `@timestamp` | Время лога | 2026-07-16T10:23:41.387Z |
| `service` | Название сервиса | accounts, cash, gateway |
| `level` | Уровень лога | DEBUG, INFO, WARN, ERROR |
| `traceId` | ID трейса | 6a57fa0fd685017f... |
| `spanId` | ID спана | af89b3dd964b5eab |
| `logger_name` | Логгер | org.hibernate.SQL |
| `thread_name` | Поток | http-nio-8080-exec-1 |
| `message` | Сообщение лога | select om1_0.id... |

---

## Примеры KQL запросов

```kql
# Логи конкретного сервиса
service: accounts

# Ошибки в сервисе
service: gateway and level: ERROR

# Логи с конкретным traceId
traceId: 6a57fa0fd685017f214dda2c9eeba17c

# SQL запросы (Hibernate)
logger_name: org.hibernate.SQL

# Логи за последние 5 минут
@timestamp > now-5m

# Комбинированный запрос
service: cash and level: ERROR and traceId: *
```

---

## Корреляция с Zipkin

1. Найти лог в Kibana с `traceId`
2. Скопировать `traceId`
3. Открыть Zipkin: http://localhost:9411/zipkin/
4. Вставить `traceId` в поиск
5. Просмотреть полный трейс across services

---

## Интеграция с Grafana

Для кросс-платформенного мониторинга:
- **Grafana:** http://localhost:30030 (метрики + алерты)
- **Kibana:** http://localhost:30561 (логи + трейсинг)
- **Zipkin:** http://localhost:9411/zipkin/ (трейсы)

---

## Troubleshooting

### Проверка статуса инициализации

```bash
# Статус Job
kubectl get job kibana-init

# Логи инициализации
kubectl logs job/kibana-init

# Если Job ещё выполняется — посмотреть логи в реальном времени
kubectl logs -f job/kibana-init
```

### Если Job не запустился

```bash
# Проверить под Kibana
kubectl get pods -l app=kibana
kubectl logs -l app=kibana

# Удалить старый Job и запустить Helm upgrade
kubectl delete job kibana-init 2>/dev/null || true
helm upgrade --install bank helm/bank -f helm/values-dev.yaml -f helm/values-secret.yaml
```

### Ручной запуск инициализации

```bash
# Удалить Job
kubectl delete job kibana-init

# Запустить Helm upgrade для пересоздания
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

# Очистка
kill %1 2>/dev/null || true
```

---

## Архитектура решения

### Helm Job (kibana-init)

Используется **Helm Hook Job** с параметрами:
- `helm.sh/hook: post-install,post-upgrade` — запускается после установки/обновления
- `helm.sh/hook-weight: "5"` — порядок выполнения (после создания Deployment)
- `helm.sh/hook-delete-policy: hook-succeeded,before-hook-creation` — автоудаление

### Почему не initContainer?

InitContainer в deployment.yaml **не работал** по следующим причинам:
1. InitContainer запускается **до** старта основного контейнера
2. Kibana ещё не готова принимать API-запросы
3. Job с retry-логикой надёжнее — ждёт доступности API

### Почему не ndjson-импорт?

Вариант с `importSavedObjects` через ConfigMap требует:
1. Создания ConfigMap с файлом
2. Монтирования volumes в Job
3. Дополнительной обработки ошибок

curl-скрипт в Job проще для отладки и даёт понятные логи.

---

## Резервный вариант: ConfigMap с ndjson

В репозитории есть `configmap-saved-objects.yaml` с готовыми объектами в формате ndjson.

Для массового импорта вручную:

```bash
# Создать ConfigMap
kubectl create configmap kibana-saved-objects \
  --from-file=saved-objects.ndjson=helm/kibana/templates/configmap-saved-objects.yaml \
  -n default

# Запустить Job для импорта
kubectl apply -f - <<EOF
apiVersion: batch/v1
kind: Job
metadata:
  name: kibana-import
spec:
  template:
    spec:
      containers:
      - name: import
        image: curlimages/curl:8.5.0
        command:
        - /bin/sh
        - -c
        - |
          curl -X POST "http://kibana:5601/api/saved_objects/_import?createNewCopies=false" \
            -H "kbn-xsrf: true" \
            --form file=@/mnt/saved-objects/saved-objects.ndjson
        volumeMounts:
        - name: saved-objects
          mountPath: /mnt/saved-objects
      volumes:
      - name: saved-objects
        configMap:
          name: kibana-saved-objects
      restartPolicy: Never
EOF
```
