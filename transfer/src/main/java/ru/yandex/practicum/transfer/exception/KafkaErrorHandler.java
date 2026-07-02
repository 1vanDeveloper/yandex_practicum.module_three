package ru.yandex.practicum.transfer.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Обработчик ошибок Kafka для Transfer сервиса.
 */
@Slf4j
@Component
public class KafkaErrorHandler {

    /**
     * Создаёт обработчик ошибок с повторными попытками.
     * 3 попытки с интервалом 1 секунда.
     */
    public DefaultErrorHandler defaultErrorHandler() {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (consumerRecord, exception) -> {
                    log.error("Обработка сообщения не удалась после всех попыток. topic={}, partition={}, offset={}, key={}",
                            consumerRecord.topic(),
                            consumerRecord.partition(),
                            consumerRecord.offset(),
                            consumerRecord.key(),
                            exception);
                },
                new FixedBackOff(1000L, 3L)
        );
        
        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                NullPointerException.class
        );
        
        return errorHandler;
    }
}
