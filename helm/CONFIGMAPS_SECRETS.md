# ConfigMaps и Secrets в Helm-чартах

## Обзор

Для каждого микросервиса и инфраструктурного компонента созданы отдельные ConfigMaps и Secrets.

## Структура

### ConfigMaps
Содержат **неконфиденциальные** настройки:
- URL сервисов (Consul, Keycloak)
- Имена клиентов OAuth2
- Настройки логирования
- Profiles Spring
- DDL mode Hibernate

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
| consul | - | - |

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
- `JWT_SECRET` — секрет JWT
- `JWT_EXPIRATION` — время жизни JWT
- `KEYCLOAK_ISSUER_URI` — URI issuer Keycloak
- `KEYCLOAK_JWK_SET_URI` — URI JWK Set

### postgresql-secrets
- `POSTGRES_USER` — пользователь
- `POSTGRES_PASSWORD` — пароль

### keycloak-secrets
- `KEYCLOAK_ADMIN` — администратор
- `KEYCLOAK_ADMIN_PASSWORD` — пароль администратора

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
kubectl get secret accounts-secrets -n bank -o jsonpath='{.data}' | base64 -d
```

### Экспорт всех ConfigMaps и Secrets
```bash
kubectl get configmap,secret -n bank -o yaml > bank-config-secrets.yaml
```
