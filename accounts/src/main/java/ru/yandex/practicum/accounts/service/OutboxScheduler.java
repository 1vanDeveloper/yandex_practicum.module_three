package ru.yandex.practicum.accounts.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "outbox.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxScheduler {

    private final OutboxProcessor outboxProcessor;

    @Scheduled(fixedRate = 10000)
    public void processOutboxMessages() {
        log.debug("Starting scheduled outbox message processing");
        try {
            outboxProcessor.processPendingMessages().join();
            log.debug("Outbox processing completed successfully");
        } catch (Exception e) {
            log.error("Outbox processing failed", e);
        }
    }
}
