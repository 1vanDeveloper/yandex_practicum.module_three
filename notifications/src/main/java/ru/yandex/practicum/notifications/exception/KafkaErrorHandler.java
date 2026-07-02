package ru.yandex.practicum.notifications.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Обработчик исключений для Kafka операций в Notifications сервисе.
 */
@Slf4j
@Component
public class KafkaErrorHandler extends DefaultErrorHandler {

    public KafkaErrorHandler() {
        super((record, exception) -> {
            log.error("Ошибка при обработке записи Kafka: topic={}, partition={}, offset={}, key={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    record.key(),
                    exception);
        }, new FixedBackOff(0L, 0L)); // Без повторных попыток
    }

    @Override
    public boolean isAckAfterHandle() {
        return false; // Не подтверждаем сообщение после ошибки
    }
}
