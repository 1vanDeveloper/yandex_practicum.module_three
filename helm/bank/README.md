# Bank Helm Chart

Зонтичный Helm-чарт для развертывания банковской платформы.

## Структура

- `accounts` - сервис управления счетами
- `cash` - сервис операций с наличными
- `frontend` - фронтенд-сервис
- `gateway` - API Gateway
- `notifications` - сервис уведомлений
- `transfer` - сервис переводов

## Установка

```bash
# Обновление зависимостей
helm dependency update

# Установка в namespace bank
helm install bank . --namespace bank --create-namespace

# Установка с кастомными значениями
helm install bank . --namespace bank -f custom-values.yaml
```

## Конфигурация

Глобальные параметры в `values.yaml`:
- `global.postgresql.*` - настройки PostgreSQL
- `global.keycloak.*` - настройки Keycloak
- `global.consul.*` - настройки Consul

Индивидуальные настройки каждого сервиса в соответствующих сабчартах.
