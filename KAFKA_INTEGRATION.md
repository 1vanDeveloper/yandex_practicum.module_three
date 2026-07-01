# Интеграция микросервисов через Apache Kafka

## Обзор

Микросервисы банковой платформы взаимодействуют через Apache Kafka вместо REST API для асинхронной, надёжной и масштабируемой коммуникации.

### Текущие интеграции

1. **Accounts → Notifications** — события учётной записи
2. **Cash → Notifications** — события транзакций (депозиты, снятия)
3. **Transfer → Notifications** — события переводов между счетами

## Архитектура

```
┌─────────────────┐         ┌──────────────────┐         ┌─────────────────────┐
│   Accounts      │         │   Apache Kafka   │         │   Notifications     │
│   Service       │         │   Cluster        │         │   Service           │
│                 │         │                  │         │                     │
│  [Producer]     │───────▶ │  Topic:          │───────▶ │  [Consumer]         │
│  KafkaNotif...  │         │  notifications   │         │  KafkaNotif...      │
│                 │         │  .events         │         │                     │
└─────────────────┘         └──────────────────┘         └─────────────────────┘

┌─────────────────┐         ┌──────────────────┐         ┌─────────────────────┐
│   Cash          │         │   Apache Kafka   │         │   Notifications     │
│   Service       │         │   Cluster        │         │   Service           │
│                 │         │                  │         │                     │
│  [Producer]     │───────▶ │  Topic:          │───────▶ │  [Consumer]         │
│  KafkaNotif...  │         │  notifications   │         │  KafkaNotif...      │
│  Sender         │         │  .events         │         │                     │
└─────────────────┘         └──────────────────┘         └─────────────────────┘

┌─────────────────┐         ┌──────────────────┐         ┌─────────────────────┐
│   Transfer      │         │   Apache Kafka   │         │   Notifications     │
│   Service       │         │   Cluster        │         │   Service           │
│                 │         │                  │         │                     │
│  [Producer]     │───────▶ │  Topic:          │───────▶ │  [Consumer]         │
│  KafkaNotif...  │         │  notifications   │         │  KafkaNotif...      │
│  Sender         │         │  .events         │         │                     │
└─────────────────┘         └──────────────────┘         └─────────────────────┘
```

## Компоненты

### Cash Service (Producer)

**KafkaConfig.java**
- Создаёт `ProducerFactory` с JSON сериализацией
- Настраивает `KafkaTemplate` для отправки событий
- Конфигурация: `acks=all`, `retries=3`, `idempotence=true`

**KafkaNotificationSender.java**
- Отправляет события `CashNotificationEvent` в топик `notifications.events`
- Асинхронная отправка с callback для логирования
- Метод `sendNotificationSync()` для синхронной отправки
- Обработка ошибок: логгирование без прерывания основного потока

**CashNotificationEvent.java**
```json
{
  "id": "uuid",
  "accountId": "account-id",
  "login": "user@example.com",
  "message": "Deposit completed: 100.00",
  "type": "DEPOSIT",
  "timestamp": "2026-06-30T10:00:00Z"
}
```

**CashService.java**
- Вызывает `kafkaNotificationSender.sendNotification()` после успешной транзакции
- Типы событий: `DEPOSIT`, `WITHDRAW`
- Безопасная отправка: ошибки не прерывают основной процесс

### Transfer Service (Producer)

**KafkaConfig.java**
- Создаёт `ProducerFactory` с JSON сериализацией
- Настраивает `KafkaTemplate` для отправки событий
- Конфигурация: `acks=all`, `retries=3`, `idempotence=true`

**KafkaNotificationSender.java**
- Отправляет события `TransferNotificationEvent` в топик `notifications.events`
- Асинхронная отправка с callback для логирования
- Метод `sendNotificationSync()` для синхронной отправки
- Обработка ошибок: логгирование без прерывания основного потока

**TransferNotificationEvent.java**
```json
{
  "id": "uuid",
  "accountId": "account-id",
  "login": "user@example.com",
  "message": "Money transferred: 100.00 to receiver",
  "type": "TRANSFER_SENT",
  "timestamp": "2026-06-30T10:00:00Z"
}
```

**TransferService.java**
- Вызывает `kafkaNotificationSender.sendNotification()` после успешного перевода
- Типы событий: `TRANSFER_SENT` (отправителю), `TRANSFER_RECEIVED` (получателю)
- Отправляет два события: одному отправителю, другое получателю
- Безопасная отправка: ошибки не прерывают основной процесс

### Accounts Service (Producer)

**KafkaNotificationProducer.java**
- Отправляет события `NotificationEvent` в топик `notifications.events`
- Асинхронная отправка с callback для логирования
- Метод `sendNotificationWithExceptionHandling()` для синхронной отправки

**OutboxProcessor.java**
- Читает сообщения из outbox таблицы
- Преобразует `OutboxMessage` в `NotificationEvent`
- Отправляет событие в Kafka
- Обновляет статус сообщения после успешной отправки

**NotificationEvent.java**
```java
{
  "id": "uuid",
  "accountId": "account-id",
  "login": "user@example.com",
  "message": "Notification text",
  "type": "ACCOUNT_NOTIFICATION",
  "timestamp": "2026-06-29T10:00:00Z"
}
```

### Notifications Service (Consumer)

**KafkaConfig.java**
- Создаёт топик `notifications.events` при старте (если не существует)
- Настраивает `ConsumerFactory` с JSON десериализацией
- Регистрирует `KafkaErrorHandler` для обработки ошибок

**KafkaNotificationConsumer.java**
- Подписывается на топик `notifications.events`
- Группа потребителей: `notifications-service`
- Вызывает `NotificationService.saveNotification()` для каждого события

**NotificationService.java**
- `saveNotification(NotificationEvent)` - сохраняет уведомление в БД

**NotificationEvent.java**
- Аналогичен событию в accounts сервисе

## Конфигурация

### application.properties (Accounts)

```properties
# Kafka Configuration (Producer)
spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:kafka:9092}
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
spring.kafka.producer.retries=3
spring.kafka.producer.acks=all
spring.kafka.producer.properties.spring.json.add.type.headers=false
```

### application.properties (Notifications)

```properties
# Kafka Configuration (Consumer)
spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:kafka:9092}
spring.kafka.consumer.group-id=notifications-service
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=*
spring.kafka.consumer.properties.spring.json.use.type.headers=false
```

## Тестирование

### Unit тесты с EmbeddedKafka

**Cash Service:**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EmbeddedKafka(
    partitions = 1,
    topics = { "notifications.events" }
)
@ContextConfiguration(classes = { KafkaConfig.class, KafkaNotificationSender.class })
class KafkaNotificationSenderTest {

    @Autowired
    private KafkaNotificationSender kafkaNotificationSender;

    @Test
    void sendNotification_shouldSendMessageToKafkaTopic() throws Exception {
        CashNotificationEvent event = CashNotificationEvent.create(
            "test_user",
            "Test notification message",
            "DEPOSIT"
        );

        // Create consumer to verify message was sent
        Consumer<String, CashNotificationEvent> consumer = ...;
        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "notifications.events");

        kafkaNotificationSender.sendNotificationSync(event);

        ConsumerRecord<String, CashNotificationEvent> record = 
            KafkaTestUtils.getSingleRecord(consumer, "notifications.events");
        
        assertEquals("test_user", record.value().getLogin());
        assertEquals("DEPOSIT", record.value().getType());
    }
}
```

**Transfer Service:**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EmbeddedKafka(
    partitions = 1,
    topics = { "notifications.events" }
)
@ContextConfiguration(classes = { KafkaConfig.class, KafkaNotificationSender.class })
class KafkaNotificationSenderTest {

    @Autowired
    private KafkaNotificationSender kafkaNotificationSender;

    @Test
    void sendNotification_shouldSendMessageToKafkaTopic() throws Exception {
        TransferNotificationEvent event = TransferNotificationEvent.create(
            "test_user",
            "Money transferred: 100.00 to receiver",
            "TRANSFER_SENT"
        );

        // Create consumer to verify message was sent
        Consumer<String, TransferNotificationEvent> consumer = ...;
        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "notifications.events");

        kafkaNotificationSender.sendNotificationSync(event);

        ConsumerRecord<String, TransferNotificationEvent> record = 
            KafkaTestUtils.getSingleRecord(consumer, "notifications.events");
        
        assertEquals("test_user", record.value().getLogin());
        assertEquals("TRANSFER_SENT", record.value().getType());
    }
}
```

**Accounts Service:**
```java
@SpringBootTest(properties = {
    "spring.kafka.bootstrap-servers=localhost:9092"
})
@Import({TestKafkaConfig.class})  // Mock для KafkaNotificationProducer
class OutboxServiceIntegrationTest {
    // Тесты используют мок producer
}
```

**Notifications Service:**
```java
@SpringBootTest(properties = {
    "spring.kafka.bootstrap-servers=localhost:9092"
})
@Import({TestKafkaConfig.class})  // Mock для KafkaNotificationConsumer
class NotificationServiceIntegrationTest {
    // Тесты используют мок consumer
}
```

### Контрактные тесты

**accounts/src/contractTest/java/.../ContractVerifierBase.java:**
```java
@Bean
public KafkaNotificationProducer kafkaNotificationProducer() {
    return Mockito.mock(KafkaNotificationProducer.class);
}
```

**notifications/src/contractTest/java/.../ContractVerifierBase.java:**
```java
@Bean
public KafkaNotificationConsumer kafkaNotificationConsumer() {
    return Mockito.mock(KafkaNotificationConsumer.class);
}
```

## NetworkPolicy (Kubernetes)

### Transfer NetworkPolicy

```yaml
egress:
  # Allow traffic to Kafka (for notifications events)
  - to:
      - podSelector:
          matchLabels:
            app.kubernetes.io/name: kafka
    ports:
      - protocol: TCP
        port: 9092
```

### Cash NetworkPolicy

```yaml
egress:
  # Allow traffic to Kafka (for notifications events)
  - to:
      - podSelector:
          matchLabels:
            app.kubernetes.io/name: kafka
    ports:
      - protocol: TCP
        port: 9092
```

### Accounts NetworkPolicy

```yaml
egress:
  # Allow traffic to Kafka (for notifications events)
  - to:
      - podSelector:
          matchLabels:
            app: kafka
    ports:
      - protocol: TCP
        port: 9092
      - protocol: TCP
        port: 9093
```

### Notifications NetworkPolicy

```yaml
ingress:
  # Allow traffic from Kafka (for notifications events)
  - from:
      - podSelector:
          matchLabels:
            app.kubernetes.io/name: kafka
    ports:
      - protocol: TCP
        port: 9092
```

**Важно:** REST доступ от accounts, cash и transfer к notifications удалён! Взаимодействие только через Kafka.

## Обработка ошибок

### Cash Service

**KafkaErrorHandler.java:**
- Создаёт `DefaultErrorHandler` с повторными попытками
- 3 попытки с интервалом 1 секунда
- Не повторяет `IllegalArgumentException` и `NullPointerException`
- Логгирует ошибки после всех попыток

**CashService.sendNotificationSafely():**
- Безопасная отправка: ошибки не прерывают транзакцию
- Логгирует предупреждения при неудаче

### Transfer Service

**KafkaErrorHandler.java:**
- Создаёт `DefaultErrorHandler` с повторными попытками
- 3 попытки с интервалом 1 секунда
- Не повторяет `IllegalArgumentException` и `NullPointerException`
- Логгирует ошибки после всех попыток

**TransferService.sendNotificationsSafely():**
- Безопасная отправка: ошибки не прерывают перевод
- Отправляет два события: отправителю и получателю
- Логгирует предупреждения при неудаче

### Accounts Service

**KafkaExceptionHandler.java:**
- Логгирует ошибки отправки в Kafka
- Позволяет реализовать retry или dead letter queue

**OutboxProcessor.handleProcessingError():**
- При ошибке отправки: увеличивает retry count
- После 3 попыток: статус FAILED

### Notifications Service

**KafkaErrorHandler.java:**
- Реализует `CommonErrorHandler`
- Логгирует ошибки обработки сообщений
- `isAckAfterHandle() = false` - не подтверждать сообщение при ошибке

## Мониторинг

### Логи

**Accounts:**
```
INFO  Отправка события в Kafka: topic=notifications.events, event={...}
INFO  Событие успешно отправлено в Kafka: topic=notifications.events, partition=0, offset=123
ERROR Ошибка при отправке события в Kafka: topic=notifications.events, event={...}
```

**Notifications:**
```
INFO  Получено событие из Kafka: topic=notifications.events, event={...}
INFO  Уведомление успешно обработано: eventId=uuid, login=user@example.com
ERROR Ошибка при обработке уведомления: eventId=uuid, login=user@example.com
```

### Метрики Kafka

- `kafka_consumer_records_consumed_total` - количество потреблённых записей
- `kafka_consumer_records_lag` - отставание потребителя
- `kafka_producer_record_send_total` - количество отправленных записей
- `kafka_producer_record_error_total` - количество ошибок отправки

## Развёртывание

### Helm Values (dev)

```yaml
kafka:
  enabled: true
  replicaCount: 1
  kafka:
    clusterId: "bank-kafka-dev"
    autoCreateTopicsEnable: true
    defaultReplicationFactor: 1
```

### Helm Values (production)

```yaml
kafka:
  enabled: true
  replicaCount: 3
  kafka:
    clusterId: "bank-kafka-prod"
    autoCreateTopicsEnable: false
    defaultReplicationFactor: 3
    transactionStateLogMinIsr: 2
```

## Преимущества Kafka перед REST

| Характеристика | REST | Kafka |
|---------------|------|-------|
| Синхронность | ✅ Синхронный | ❌ Асинхронный |
| Надёжность | ❌ Требует retry логики | ✅ Гарантированная доставка |
| Масштабируемость | ❌ Point-to-point | ✅ Pub/Sub модель |
| Буферизация | ❌ Нет | ✅ До 7 дней (настраивается) |
| Нагрузка | ❌ Пиковая нагрузка на сервис | ✅ Сглаживание пиков |
| Аудит | ❌ Требует отдельного логирования | ✅ Все события в топике |

## Troubleshooting

### Producer не отправляет сообщения

1. Проверить подключение к Kafka:
   ```bash
   kubectl exec -it accounts-xxx -- nc -zv kafka:9092
   ```

2. Проверить логи:
   ```bash
   kubectl logs -l app=accounts | grep -i kafka
   ```

### Consumer не получает сообщения

1. Проверить группу потребителей:
   ```bash
   kubectl exec -it kafka-0 -- kafka-consumer-groups.sh \
     --bootstrap-server localhost:9092 \
     --describe --group notifications-service
   ```

2. Проверить offset:
   ```bash
   kubectl exec -it kafka-0 -- kafka-consumer-groups.sh \
     --bootstrap-server localhost:9092 \
     --describe --group notifications-service --verbose
   ```

### Топик не создан

1. Проверить наличие топика:
   ```bash
   kubectl exec -it kafka-0 -- kafka-topics.sh \
     --bootstrap-server localhost:9092 --list
   ```

2. Создать вручную:
   ```bash
   kubectl exec -it kafka-0 -- kafka-topics.sh \
     --bootstrap-server localhost:9092 \
     --create --topic notifications.events \
     --partitions 3 --replication-factor 3
   ```
