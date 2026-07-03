package ru.yandex.practicum.notifications.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Обработчик исключений для Kafka операций в Notifications сервисе.
 * 
 * Стратегия обработки ошибок:
 * - 3 повторные попытки с интервалом 1 секунда
 * - После исчерпания попыток: логирование и подтверждение сообщения (ack)
 * - isAckAfterHandle() = false: не подтверждаем до завершения всех retry
 */
@Slf4j
@Component
public class KafkaErrorHandler extends DefaultErrorHandler {

    private static final long BACKOFF_INTERVAL = 1000L; // 1 секунда
    private static final long MAX_ATTEMPTS = 3L;

    public KafkaErrorHandler() {
        super((record, exception) -> {
            log.error("Ошибка при обработке записи Kafka после исчерпания retry: " +
                            "topic={}, partition={}, offset={}, key={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    record.key(),
                    exception);
        }, new FixedBackOff(BACKOFF_INTERVAL, MAX_ATTEMPTS));
    }

    @Override
    public boolean isAckAfterHandle() {
        return true; // Подтверждаем сообщение после исчерпания retry
    }
}
