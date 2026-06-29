package ru.yandex.practicum.notifications.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.stereotype.Component;

/**
 * Обработчик исключений для Kafka операций в Notifications сервисе.
 */
@Slf4j
@Component
public class KafkaErrorHandler implements CommonErrorHandler {

    @Override
    public boolean isAckAfterHandle() {
        return false; // Не подтверждаем сообщение после ошибки
    }
}
