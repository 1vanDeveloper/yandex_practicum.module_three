# Jenkins CI/CD Pipeline Documentation

## Обзор

Проект использует многоуровневую CI/CD архитектуру с поддержкой:
- **Individual pipelines** — сборка и деплой каждого микросервиса отдельно
- **Umbrella pipeline** — сборка и деплой всех микросервисов сразу

## Структура Jenkinsfile

```
├── Jenkinsfile                    # Umbrella pipeline (все сервисы)
├── accounts/Jenkinsfile           # Accounts Service
├── cash/Jenkinsfile               # Cash Service
├── transfer/Jenkinsfile           # Transfer Service
├── notifications/Jenkinsfile      # Notifications Service
├── gateway/Jenkinsfile            # Gateway Service
└── frontend/Jenkinsfile           # Frontend Service
```

## Окружения

| Окружение | Namespace | Описание |
|-----------|-----------|----------|
| **dev** | `bank-dev` | Разработка и интеграционное тестирование |
| **staging** | `bank-staging` | Предпродакшен, финальное тестирование |
| **production** | `bank-production` | Боевое окружение (требует подтверждения) |

## Parameters

### Umbrella Pipeline

| Параметр | Тип | Значения | Описание |
|----------|-----|----------|----------|
| `DEPLOY_ENV` | choice | dev, staging, production | Окружение для деплоя |
| `RUN_TESTS` | boolean | true/false | Запускать ли тесты |
| `PARALLEL_BUILD` | boolean | true/false | Параллельная сборка сервисов |
| `GIT_COMMIT` | string | hash | Конкретный коммит для сборки |

### Individual Pipeline

| Параметр | Тип | Значения | Описание |
|----------|-----|----------|----------|
| `DEPLOY_ENV` | choice | none, dev, staging, production | Окружение для деплоя |
| `RUN_TESTS` | boolean | true/false | Запускать ли unit-тесты |
| `RUN_CONTRACT_TESTS` | boolean | true/false | Запускать ли контрактные тесты |
| `GIT_COMMIT` | string | hash | Конкретный коммит для сборки |

## Этапы Pipeline

### 1. Preparation
```groovy
├── Checkout              // Git checkout
├── Install Tools         // Helm, kubectl
└── Validate
    ├── Validate Helm Charts
    └── Validate Gradle
```

### 2. Build & Test (Parallel)
```groovy
├── Accounts Service
├── Cash Service
├── Transfer Service
├── Notifications Service
├── Gateway Service
└── Frontend Service
```

Каждый сервис проходит:
- Сборку (`./gradlew :service:build`)
- Unit тесты (`./gradlew :service:test`)
- Контрактные тесты (`./gradlew :service:contractTest`)
- Интеграционные тесты (`./gradlew :service:test --tests "*IntegrationTest*"`)

### 3. Docker Build & Push
```bash
docker build -t registry/bank-{service}:${BUILD_VERSION}
docker tag registry/bank-{service}:${BUILD_VERSION} registry/bank-{service}:latest
docker push registry/bank-{service}:${BUILD_VERSION}
docker push registry/bank-{service}:latest
```

### 4. Deploy to Kubernetes
```bash
helm upgrade --install bank helm/bank \
    --namespace bank-{env} \
    --values helm/values-{env}.yaml \
    --set global.imageTag=${BUILD_VERSION} \
    --timeout 10m --wait --atomic
```

### 5. Post-Deployment Validation
```bash
kubectl wait --for=condition=ready pod -l app.kubernetes.io/part-of=bank
kubectl get pods -l app.kubernetes.io/part-of=bank
kubectl get svc -l app.kubernetes.io/part-of=bank
```

## Jenkins Job Configuration

### Umbrella Job

```groovy
pipelineJob('bank-platform-umbrella') {
    description('Umbrella pipeline для всех микросервисов')
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url('https://github.com/1vanDeveloper/yandex_practicum.module_three.git')
                    }
                    branch('*/sprint_*')
                }
            }
            scriptPath('Jenkinsfile')
        }
    }
    
    properties {
        parameters {
            choiceParam('DEPLOY_ENV', ['dev', 'staging', 'production'])
            booleanParam('RUN_TESTS', true)
            booleanParam('PARALLEL_BUILD', true)
        }
    }
}
```

### Individual Jobs

```groovy
pipelineJob('bank-accounts') {
    description('CI/CD pipeline для Accounts Service')
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url('https://github.com/1vanDeveloper/yandex_practicum.module_three.git')
                    }
                    branch('*/sprint_*')
                }
            }
            scriptPath('accounts/Jenkinsfile')
        }
    }
}
```

## Триггеры

### Автоматические
- Push в ветку `sprint_*` → запуск Individual pipeline
- Merge в `main` → запуск Umbrella pipeline с `DEPLOY_ENV=dev`

### Ручные
- Деплой в staging/production через Jenkins UI
- Выбор конкретного коммита для сборки

## Notifications

### Уведомления об успеха/ошибке
```groovy
post {
    success {
        slackSend(channel: '#bank-ci-cd', color: 'good', message: "Build ${env.BUILD_NUMBER} successful!")
    }
    failure {
        slackSend(channel: '#bank-ci-cd', color: 'danger', message: "Build ${env.BUILD_NUMBER} failed!")
    }
}
```

## Требования к Jenkins

### Плагины
- Pipeline
- Git
- Gradle
- Docker Pipeline
- Kubernetes CLI
- Helm
- JUnit
- Slack Notification

### Credentials

| ID | Type | Описание |
|----|------|----------|
| `docker-registry-creds` | Username/Password | Docker Registry |
| `k8s-kubeconfig` | Secret file | Kubernetes config |
| `slack-webhook` | Secret text | Slack webhook URL |

## Values файлы

| Файл | Окружение | Описание |
|------|-----------|----------|
| `helm/values-dev.yaml` | dev | Минимальные ресурсы, NodePort |
| `helm/values-staging.yaml` | staging | 2 реплики, LoadBalancer, Ingress |
| `helm/values-production.yaml` | production | 3+ реплики, PDB, RollingUpdate, TLS |

## Безопасность

### Production деплой
- Требует ручного подтверждения (`input` step)
- Использует конкретную версию образа (не `latest`)
- Secrets из External Secrets Manager

### Secrets Management
```bash
# Не коммитить secrets в git!
helm/values-secret.yaml  # В .gitignore
helm/values-secret.yaml.example  # Шаблон для команды

# Использование
helm upgrade bank helm/bank -f helm/values-secret.yaml
```

## Мониторинг

### Health Checks
```bash
# Проверка после деплоя
kubectl wait --for=condition=ready pod -l app.kubernetes.io/part-of=bank --timeout=300s

# Проверка health endpoints
curl http://gateway:8080/actuator/health
curl http://accounts:8080/actuator/health
```

### Logs
```bash
# Просмотр логов
kubectl logs -l app=accounts -f
kubectl logs -l app=gateway -f
```

## Rollback

```bash
# Откат к предыдущей версии
helm rollback bank -n bank-production

# Откат к конкретной ревизии
helm rollback bank 5 -n bank-production
```

## Примеры использования

### Деплой конкретного сервиса в dev
```bash
# Jenkins UI: bank-accounts job
# Parameters: DEPLOY_ENV=dev, RUN_TESTS=true
```

### Деплой всех сервисов в staging
```bash
# Jenkins UI: bank-platform-umbrella job
# Parameters: DEPLOY_ENV=staging, RUN_TESTS=true, PARALLEL_BUILD=true
```

### Production деплой
```bash
# Jenkins UI: bank-platform-umbrella job
# Parameters: DEPLOY_ENV=production
# → Требуется подтверждение "Deploy to Production"
```
