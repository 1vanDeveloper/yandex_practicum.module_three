# ConfigMaps и Secrets в Helm-чартах

## Обзор

Для каждого микросервиса и инфраструктурного компонента созданы отдельные ConfigMaps и Secrets.
**Consul удалён** — используется Kubernetes-native Service Discovery.

## Архитектура

### Service Discovery
- **Kubernetes Services** — DNS-имена сервисов (`accounts`, `cash`, `gateway`, etc.)
- **Spring Cloud LoadBalancer** — балансировка между подами
- **Spring Cloud Gateway** — маршрутизация через lb://service-name

### Конфигурация
- **ConfigMaps** — неконфиденциальные настройки (URL, имена, логирование)
- **Secrets** — конфиденциальные данные (пароли, секреты, токены)

## Структура

### ConfigMaps
Содержат **неконфиденциальные** настройки:
- URL сервисов (PostgreSQL, Keycloak)
- Имена клиентов OAuth2
- Настройки логирования
- Profiles Spring
- DDL mode Hibernate
- Настройки Spring Cloud Gateway

### Secrets
Содержат **конфиденциальные** данные:
- Пароли PostgreSQL
- JWT секреты
- OAuth2 client secrets
- Учётные данные Keycloak

## Микросервисы

| Сервис | ConfigMap | Secret |
|--------|-----------|--------|
| accounts | accounts-config | accounts-secrets |
| cash | cash-config | cash-secrets |
| frontend | frontend-config | frontend-secrets |
| gateway | gateway-config | gateway-secrets |
| notifications | notifications-config | notifications-secrets |
| transfer | transfer-config | transfer-secrets |

## Инфраструктура

| Компонент | ConfigMap | Secret |
|-----------|-----------|--------|
| postgresql | postgresql-init-scripts | postgresql-secrets |
| keycloak | keycloak-realm-config | keycloak-secrets |

## Использование в Deployment

Переменные окружения подключаются через `valueFrom`:

```yaml
env:
  # Из ConfigMap
  - name: SPRING_PROFILES_ACTIVE
    valueFrom:
      configMapKeyRef:
        name: accounts-config
        key: SPRING_PROFILES_ACTIVE
  
  # Из Secret
  - name: SPRING_DATASOURCE_PASSWORD
    valueFrom:
      secretKeyRef:
        name: accounts-secrets
        key: POSTGRES_PASSWORD
```

## Ключи в Secrets

### accounts-secrets, cash-secrets, transfer-secrets
- `POSTGRES_USERNAME` — пользователь БД
- `POSTGRES_PASSWORD` — пароль БД
- `JWT_SECRET` — секрет JWT
- `JWT_EXPIRATION` — время жизни JWT
- `OAUTH2_CLIENT_SECRET` — секрет OAuth2 клиента
- `KEYCLOAK_ISSUER_URI` — URI issuer Keycloak
- `KEYCLOAK_JWK_SET_URI` — URI JWK Set

### frontend-secrets
- `OAUTH2_CLIENT_SECRET` — секрет OAuth2 клиента
- `KEYCLOAK_ISSUER_URI` — URI issuer Keycloak
- `KEYCLOAK_JWK_SET_URI` — URI JWK Set

### gateway-secrets
- `JWT_SECRET` — секрет JWT
- `JWT_EXPIRATION` — время жизни JWT
- `OAUTH2_CLIENT_SECRET` — секрет OAuth2 клиента
- `KEYCLOAK_ISSUER_URI` — URI issuer Keycloak
- `KEYCLOAK_JWK_SET_URI` — URI JWK Set

### notifications-secrets
- `POSTGRES_USERNAME` — пользователь БД
- `POSTGRES_PASSWORD` — пароль БД
- `KEYCLOAK_ISSUER_URI` — URI issuer Keycloak
- `KEYCLOAK_JWK_SET_URI` — URI JWK Set

**Примечание:** JWT секреты удалены (нет REST API, только Kafka consumer)

### postgresql-secrets
- `POSTGRES_USER` — пользователь
- `POSTGRES_PASSWORD` — пароль

### keycloak-secrets
- `KEYCLOAK_ADMIN` — администратор
- `KEYCLOAK_ADMIN_PASSWORD` — пароль администратора

## Service Discovery без Consul

### DNS-имена сервисов

Каждый сервис доступен по DNS-имени в кластере:

```
<service-name>.<namespace>.svc.cluster.local
```

Примеры:
- `accounts` — сервис accounts в том же namespace
- `accounts.default.svc.cluster.local` — полное имя
- `postgresql` — база данных
- `keycloak` — сервер авторизации
- `gateway` — API Gateway

### Spring Cloud Gateway маршруты

```yaml
spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lowerCaseServiceId: true
```

Маршруты автоматически создаются по имени сервиса:
- `lb://accounts` → сервис accounts
- `lb://cash` → сервис cash
- `lb://transfer` → сервис transfer

### Kubernetes Service для каждого сервиса

```yaml
apiVersion: v1
kind: Service
metadata:
  name: accounts
spec:
  selector:
    app: accounts
  ports:
    - port: 8080
      targetPort: 8080
```

## Управление секретами

### Создание Secret вручную

```bash
# Создание Secret из literal значений
kubectl create secret generic accounts-secrets \
  --from-literal=POSTGRES_PASSWORD='secure-password' \
  --from-literal=JWT_SECRET='secure-jwt-secret' \
  --namespace=bank

# Создание Secret из файла
kubectl create secret generic accounts-secrets \
  --from-file=.env \
  --namespace=bank
```

### Обновление Secret

```bash
# Удаление и создание заново
kubectl delete secret accounts-secrets -n bank
kubectl create secret generic accounts-secrets \
  --from-literal=POSTGRES_PASSWORD='new-password' \
  -n bank

# Перезапуск Deployment для применения нового Secret
kubectl rollout restart deployment/accounts -n bank
```

### External Secrets Operator

Для интеграции с внешними системами (HashiCorp Vault, AWS Secrets Manager):

```yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: accounts-secrets
  namespace: bank
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: vault-backend
    kind: ClusterSecretStore
  target:
    name: accounts-secrets
  data:
    - secretKey: POSTGRES_PASSWORD
      remoteRef:
        key: bank/accounts
        property: db-password
```

## Безопасность

1. **Шифрование etcd**: Включите шифрование Secret в etcd
2. **RBAC**: Ограничьте доступ к Secret через Role/RoleBinding
3. **Network Policies**: Ограничьте сетевой доступ к подам
4. **Secrets Rotation**: Регулярно обновляйте секреты

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: secrets-reader
  namespace: bank
rules:
  - apiGroups: [""]
    resources: ["secrets"]
    verbs: ["get", "list"]
    resourceNames: ["accounts-secrets", "cash-secrets"]
```

## Примеры использования

### Просмотр ConfigMap
```bash
kubectl get configmap accounts-config -n bank -o yaml
```

### Просмотр Secret (требуются права)
```bash
kubectl get secret accounts-secrets -n bank -o jsonpath='{.data}'
```

### Экспорт всех ConfigMaps и Secrets
```bash
kubectl get configmap,secret -n bank -o yaml > bank-config-secrets.yaml
```

### Проверка переменных окружения в поде
```bash
kubectl exec -it <pod-name> -n bank -- env | grep SPRING
```

## Миграция с Consul

### До изменений
```yaml
env:
  - name: SPRING_CLOUD_CONSUL_HOST
    value: "consul"
  - name: SPRING_CLOUD_CONSUL_PORT
    value: "8500"
  - name: SPRING_CLOUD_CONSUL_DISCOVERY_ENABLED
    value: "true"
```

### После изменений
```yaml
# Consul удалён
# Service Discovery через Kubernetes DNS
# Spring Cloud LoadBalancer вместо Consul
```

### Обновление application.properties
```properties
# Было (с Consul)
spring.cloud.consul.host=consul
spring.cloud.consul.port=8500
spring.cloud.consul.discovery.enabled=true

# Стало (Kubernetes-native)
# Service Discovery через Kubernetes DNS
# spring.cloud.kubernetes.discovery.enabled=true (опционально)
```
