package ru.yandex.practicum.accounts.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Обработчик исключений для Kafka операций в Accounts сервисе.
 */
@Slf4j
@Component
public class KafkaExceptionHandler {

    /**
     * Обрабатывает ошибки при отправке сообщений в Kafka.
     */
    public <T> void handleSendError(CompletableFuture<SendResult<String, T>> future, String topic, Object event) {
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Kafka send failed: topic={}, event={}", topic, event, ex);
                
                if (ex.getCause() instanceof KafkaException) {
                    log.error("Kafka exception: {}", ex.getCause().getMessage());
                }
                
                // Здесь можно добавить логику для повторной отправки или сохранения в dead letter queue
            }
        });
    }
}
