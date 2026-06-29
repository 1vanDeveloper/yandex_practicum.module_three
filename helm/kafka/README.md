# Kafka Helm Chart Documentation

## Обзор

Apache Kafka кластер для асинхронной коммуникации между микросервисами банковской платформы.

**Особенности:**
- KRaft mode (без ZooKeeper) - Kafka 3.x
- StatefulSet для стабильности подов
- Headless сервис для прямого подключения к брокерам
- NetworkPolicy для безопасности
- PodDisruptionBudget для высокой доступности
- ServiceMonitor для Prometheus мониторинга

## Архитектура

```
┌─────────────────────────────────────────────────────────────────┐
│                        Kubernetes Cluster                        │
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                    Kafka StatefulSet                      │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐       │  │
│  │  │  kafka-0    │  │  kafka-1    │  │  kafka-2    │       │  │
│  │  │  :9092      │  │  :9092      │  │  :9092      │       │  │
│  │  │  :9093      │  │  :9093      │  │  :9093      │       │  │
│  │  │  PVC: 10Gi  │  │  PVC: 10Gi  │  │  PVC: 10Gi  │       │  │
│  │  └─────────────┘  └─────────────┘  └─────────────┘       │  │
│  │                                                           │  │
│  │  ┌─────────────────────────────────────────────────────┐ │  │
│  │  │         Kafka Service (ClusterIP)                   │ │  │
│  │  │         kafka:9092, kafka-controller:9093           │ │  │
│  │  └─────────────────────────────────────────────────────┘ │  │
│  │                                                           │  │
│  │  ┌─────────────────────────────────────────────────────┐ │  │
│  │  │      Kafka Headless Service (ClusterIP: None)       │ │  │
│  │  │      kafka-headless:9092, kafka-headless:9093       │ │  │
│  │  └─────────────────────────────────────────────────────┘ │  │
│  │                                                           │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │              Микросервисы (Clients)                       │  │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐                │  │
│  │  │ accounts │  │   cash   │  │ transfer │                │  │
│  │  └────┬─────┘  └────┬─────┘  └────┬─────┘                │  │
│  │       │            │            │                        │  │
│  │       └────────────┴────────────┘                        │  │
│  │                    │                                      │  │
│  │                    ▼                                      │  │
│  │           ┌─────────────────┐                            │  │
│  │           │  notifications  │ ← Kafka Consumer           │  │
│  │           └────────┬────────┘                            │  │
│  │                    │                                      │  │
│  │                    ▼                                      │  │
│  │           ┌─────────────────┐                            │  │
│  │           │  Kafka Cluster  │                            │  │
│  │           └─────────────────┘                            │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## Установка

### Dev окружение (1 реплика)

```bash
helm upgrade --install kafka helm/kafka \
  --namespace bank-dev \
  --create-namespace \
  --values helm/values-dev.yaml
```

### Staging окружение (3 реплики)

```bash
helm upgrade --install kafka helm/kafka \
  --namespace bank-staging \
  --create-namespace \
  --values helm/values-staging.yaml
```

### Production окружение (3 реплики)

```bash
helm upgrade --install kafka helm/kafka \
  --namespace bank-production \
  --create-namespace \
  --values helm/values-production.yaml
```

## Проверка статуса

```bash
# Проверка подов
kubectl get pods -l app.kubernetes.io/name=kafka -n bank-dev

# Проверка сервиса
kubectl get svc -l app.kubernetes.io/name=kafka -n bank-dev

# Проверка StatefulSet
kubectl get statefulset kafka -n bank-dev

# Проверка PVC
kubectl get pvc -l app.kubernetes.io/name=kafka -n bank-dev

# Ожидание готовности
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=kafka --timeout=300s -n bank-dev
```

## Подключение

### Изнутри кластера

```bash
# Bootstrap серверы
kafka:9092

# Headless для прямого подключения
kafka-0.kafka-headless:9092
kafka-1.kafka-headless:9092
kafka-2.kafka-headless:9092
```

### Пример подключения из микросервиса

```yaml
# deployment.yaml микросервиса
env:
  - name: SPRING_KAFKA_BOOTSTRAP_SERVERS
    value: "kafka:9092"
  - name: SPRING_KAFKA_PRODUCER_RETRIES
    value: "3"
  - name: SPRING_KAFKA_CONSUMER_AUTO_OFFSET_RESET
    value: "earliest"
```

### Kafka CLI утилиты

```bash
# Создать топик
kubectl exec -it kafka-0 -n bank-dev -- \
  kafka-topics.sh --bootstrap-server localhost:9092 \
  --create --topic notifications --partitions 3 --replication-factor 3

# Список топиков
kubectl exec -it kafka-0 -n bank-dev -- \
  kafka-topics.sh --bootstrap-server localhost:9092 --list

# Описание топика
kubectl exec -it kafka-0 -n bank-dev -- \
  kafka-topics.sh --bootstrap-server localhost:9092 \
  --describe --topic notifications

# Producer тест
kubectl exec -it kafka-0 -n bank-dev -- \
  kafka-console-producer.sh --bootstrap-server localhost:9092 --topic test-topic

# Consumer тест
kubectl exec -it kafka-0 -n bank-dev -- \
  kafka-console-consumer.sh --bootstrap-server localhost:9092 \
  --topic test-topic --from-beginning

# Проверка брокеров
kubectl exec -it kafka-0 -n bank-dev -- \
  kafka-broker-api-versions --bootstrap-server localhost:9092
```

## Конфигурация

### Основные параметры

| Параметр | Dev | Staging | Production |
|----------|-----|---------|------------|
| Реплики | 1 | 3 | 3 |
| Репликация топиков | 1 | 3 | 3 |
| Min ISR | 1 | 2 | 2 |
| Хранилище | 5Gi | 20Gi | 50Gi |
| CPU (request/limit) | 500m/1000m | 1000m/2000m | 2000m/4000m |
| Memory (request/limit) | 512Mi/1024Mi | 1024Mi/2048Mi | 2048Mi/4096Mi |
| Auto-create topics | ✅ | ✅ | ❌ |
| PDB | ❌ | ✅ (min=2) | ✅ (min=2) |
| NetworkPolicy | ✅ | ✅ | ✅ |

### Топики для микросервисов

```bash
# Топик для уведомлений (notifications service)
kubectl exec -it kafka-0 -n bank-dev -- \
  kafka-topics.sh --bootstrap-server localhost:9092 \
  --create --topic notifications \
  --partitions 3 --replication-factor 3 \
  --config retention.ms=604800000  # 7 дней

# Топик для аудита (audit events)
kubectl exec -it kafka-0 -n bank-dev -- \
  kafka-topics.sh --bootstrap-server localhost:9092 \
  --create --topic audit-events \
  --partitions 6 --replication-factor 3 \
  --config retention.ms=2592000000  # 30 дней

# Топик для транзакций (transaction events)
kubectl exec -it kafka-0 -n bank-dev -- \
  kafka-topics.sh --bootstrap-server localhost:9092 \
  --create --topic transactions \
  --partitions 6 --replication-factor 3 \
  --config retention.ms=604800000  # 7 дней
```

## Мониторинг

### Prometheus метрики

Kafka экспортирует метрики через JMX. Для сбора метрик включите ServiceMonitor:

```yaml
# values.yaml
serviceMonitor:
  enabled: true
  namespace: monitoring
  interval: 30s
  scrapeTimeout: 10s
```

### Ключевые метрики

- `kafka_server_brokertopicmetrics_bytesin_total` - входящий трафик
- `kafka_server_brokertopicmetrics_bytesout_total` - исходящий трафик
- `kafka_server_brokertopicmetrics_messagesin_total` - количество сообщений
- `kafka_server_replicamanager_underreplicatedpartitions` - недостаточнореплицированные партиции
- `kafka_controller_kafkacontroller_offlinepartitionscount` - оффлайн партиции
- `kafka_network_requestmetrics_requestqueuetimems` - время в очереди запросов

### Grafana Dashboard

Импортируйте дашборд [Kafka Overview](https://grafana.com/grafana/dashboards/7589-kafka-overview/) для визуализации метрик.

## Безопасность

### NetworkPolicy

```yaml
# Разрешить трафик только от микросервисов
networkPolicy:
  enabled: true
  allowExternal: false
  allowNamespaces:
    - bank-dev
```

### TLS шифрование (Production)

Для production рекомендуется включить TLS:

```yaml
# values-production.yaml
kafka:
  listeners:
    - "SSL://0.0.0.0:9093"
    - "CONTROLLER://0.0.0.0:9094"
  advertisedListeners:
    - "SSL://kafka-0.kafka-headless:9093"
    # ...
```

## Backup и восстановление

### Backup топиков

```bash
# Создать snapshot топика
kubectl exec -it kafka-0 -n bank-dev -- \
  kafka-dump-log.sh --deep-iteration \
  --files /var/lib/kafka/data/00000000000000000000.log \
  > topic-backup.txt
```

### Восстановление из backup

```bash
# Восстановить топик из backup
kubectl exec -i kafka-0 -n bank-dev -- \
  kafka-console-producer.sh --bootstrap-server localhost:9092 --topic restored-topic \
  < topic-backup.txt
```

## Troubleshooting

### Проблемы с подключением

```bash
# Проверить логи
kubectl logs kafka-0 -n bank-dev

# Проверить сетевую связность
kubectl exec -it kafka-0 -n bank-dev -- \
  nc -zv kafka-1.kafka-headless 9092

# Проверить состояние брокера
kubectl exec -it kafka-0 -n bank-dev -- \
  kafka-broker-api-versions --bootstrap-server localhost:9092
```

### Проблемы с репликацией

```bash
# Проверить недостаточнореплицированные партиции
kubectl exec -it kafka-0 -n bank-dev -- \
  kafka-topics.sh --bootstrap-server localhost:9092 \
  --describe --under-replicated-partitions

# Проверить оффлайн партиции
kubectl exec -it kafka-0 -n bank-dev -- \
  kafka-topics.sh --bootstrap-server localhost:9092 \
  --describe --unavailable-partitions
```

### Перезапуск Kafka

```bash
# Rolling restart StatefulSet
kubectl rollout restart statefulset kafka -n bank-dev

# Проверка статуса
kubectl rollout status statefulset kafka -n bank-dev
```

## Интеграция с микросервисами

### Notifications Service (Producer)

```java
@Service
public class NotificationProducer {
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    
    public NotificationProducer(KafkaTemplate<String, NotificationEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    public void send(NotificationEvent event) {
        kafkaTemplate.send("notifications", event.getAccountId(), event);
    }
}
```

### Notifications Service (Consumer)

```java
@Service
public class NotificationConsumer {
    @KafkaListener(topics = "notifications", groupId = "notifications-service")
    public void listen(NotificationEvent event) {
        // Обработка уведомления
        log.info("Received notification: {}", event);
    }
}
```

### Конфигурация Spring Boot

```yaml
# application.yaml
spring:
  kafka:
    bootstrap-servers: ${SPRING_KAFKA_BOOTSTRAP_SERVERS:kafka:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      retries: 3
      acks: all
    consumer:
      group-id: notifications-service
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
```

## Масштабирование

### Увеличение количества брокеров

```bash
# Изменить количество реплик
helm upgrade kafka helm/kafka \
  --set replicaCount=5 \
  --namespace bank-dev

# Проверить новые поды
kubectl get pods -l app.kubernetes.io/name=kafka -n bank-dev
```

### Увеличение партиций

```bash
# Увеличить количество партиций топика
kubectl exec -it kafka-0 -n bank-dev -- \
  kafka-topics.sh --bootstrap-server localhost:9092 \
  --alter --topic notifications --partitions 6
```

## Удаление

```bash
# Удалить релиз
helm uninstall kafka -n bank-dev

# Удалить PVC (данные будут потеряны!)
kubectl delete pvc -l app.kubernetes.io/name=kafka -n bank-dev

# Удалить ConfigMaps и Secrets
kubectl delete configmap -l app.kubernetes.io/name=kafka -n bank-dev
kubectl delete secret -l app.kubernetes.io/name=kafka -n bank-dev
```
