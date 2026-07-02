# Kafka Helm chart

## Prerequisites

- Kubernetes 1.18+
- Helm 3.3.0+

## Примеры развёртывания

```shell
# Добавить Helm репозиторий
helm repo add kafka-repo https://helm-charts.itboon.top/kafka
helm repo update kafka-repo
```

```shell
# Развёртывание одноузлового кластера (один Pod)
# Персистентное хранилище отключено для демонстрации
helm upgrade --install kafka \
  --namespace kafka-demo \
  --create-namespace \
  --set broker.combinedMode.enabled="true" \
  --set broker.persistence.enabled="false" \
  kafka-repo/kafka

# Развёртывание кластера 1 controller + 1 broker с персистентным хранилищем
helm upgrade --install kafka \
  --namespace kafka-demo \
  --create-namespace \
  --set broker.persistence.size="20Gi" \
  kafka-repo/kafka

# Развёртывание отказоустойчивого кластера: 3 controller + 3 broker
helm upgrade --install kafka \
  --namespace kafka-demo \
  --create-namespace \
  --set controller.replicaCount="3" \
  --set broker.replicaCount="3" \
  --set broker.heapOpts="-Xms4096m -Xmx4096m" \
  --set broker.resources.requests.memory="6Gi" \
  kafka-repo/kafka

# Подробные значения values для производственного кластера см. https://github.com/sir5kong/kafka-docker/raw/main/examples/values-production.yml


# Внешняя доступность через LoadBalancer
helm upgrade --install kafka \
  --namespace kafka-demo \
  --create-namespace \
  --set broker.external.enabled="true" \
  --set broker.external.service.type="LoadBalancer" \
  --set broker.external.domainSuffix="kafka.example.com" \
  kafka-repo/kafka

# Внешняя доступность через NodePort
helm upgrade --install kafka \
  --namespace kafka-demo \
  --create-namespace \
  -f https://github.com/sir5kong/kafka-docker/raw/main/examples/values-nodeport.yml \
  kafka-repo/kafka

```

## Смешанный режим `combined mode`

`process.roles` может принимать значения `broker`, `controller`, `broker,controller`. Значение `broker,controller` включает смешанный режим.

Развёртывание и управление серверами в смешанном режиме проще, но масштабирование затруднено. Например, при развёртывании отказоустойчивого кластера с 3 controller и 3 broker, последующее масштабирование broker возможно без прерывания работы. В смешанном режиме масштабирование сложнее и требует простоя.

Оригинальный текст из документации:

> Kafka servers that act as both brokers and controllers are referred to as "combined" servers. Combined servers are simpler to operate for small use cases like a development environment. The key disadvantage is that the controller will be less isolated from the rest of the system. For example, it is not possible to roll or scale the controllers separately from the brokers in combined mode. Combined mode is not recommended in critical deployment environments.

### Параметры Chart

| Key | Тип | Значение по умолчанию | Описание |
|-----|------|---------|-------------|
| broker.combinedMode.enabled | bool | `false` | Включить смешанный режим |

```yaml
# Пример смешанного режима
broker:
  combinedMode:
    enabled: true
  replicaCount: 1
  heapOpts: "-Xms1024m -Xmx1024m"
  persistence:
    enabled: true
    size: 20Gi
```

## Cluster ID

В ранних версиях Kafka автоматически инициализировала каталоги данных. В текущих версиях требуется предоставить `Cluster ID` и вручную инициализировать каталоги:

```shell
KAFKA_CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c config/kraft/server.properties
```

Все узлы кластера должны использовать один `Cluster ID`. Этот chart автоматически генерирует `Cluster ID` при первом развёртывании и сохраняет его в `Secret`.

## node.id

`Cluster ID` общий для всех узлов кластера, а `node.id` должен быть уникальным для каждого узла.

Например, в кластере `3 controller 3 broker` controller имеют `node.id` равные `0`, `1`, `2` (соответствуют номерам Pod StatefulSet), но broker не могут использовать те же id. Поэтому введён параметр `KAFKA_NODE_ID_OFFSET` со значением по умолчанию `1000`. Broker получают `node.id` равные `1000`, `1001`, `1002`.

## Внешняя доступность

Для подключения к Kafka извне кластера необходимо expose каждый Broker и правильно настроить `advertised.listeners`.

Поддерживаются два способа: `NodePort` и `LoadBalancer`. Количество broker узлов должно соответствовать количеству `NodePort` или `LoadBalancer`.

### Параметры Chart

| Key | Тип | Значение по умолчанию | Описание |
|-----|------|---------|-------------|
| broker.external.enabled | bool | `false` | Включить внешнюю доступность |
| broker.external.service.type | string | `NodePort` | Тип внешней доступности: `NodePort` или `LoadBalancer` |
| broker.external.service.annotations | object | `{}` | Аннотации для внешнего service |
| broker.external.nodePorts | list | `[]` | Порты NodePort. Требуется минимум один порт. Если портов меньше чем broker узлов, они автоматически нумеруются |
| broker.external.domainSuffix | string | `kafka.example.com` | Требуется домен для LoadBalancer. Внешний домен broker: `POD_NAME` + `доменный суффикс`, например `kafka-broker-0.kafka.example.com`. После развёртывания необходимо настроить DNS |

```yaml
# Пример NodePort
broker:
  replicaCount: 3
  external:
    enabled: true
    service:
      type: "NodePort"
      annotations: {}
    nodePorts:
      - 31050
      - 31051
      - 31052
```

```yaml
# Пример LoadBalancer
broker:
  replicaCount: 3
  external:
    enabled: true
    service:
      type: "LoadBalancer"
      annotations: {}
    domainSuffix: "kafka.example.com"
```
