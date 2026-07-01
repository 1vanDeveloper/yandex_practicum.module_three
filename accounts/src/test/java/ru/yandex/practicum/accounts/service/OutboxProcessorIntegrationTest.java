package ru.yandex.practicum.accounts.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import ru.yandex.practicum.accounts.config.TestExceptionHandlerConfig;
import ru.yandex.practicum.accounts.config.TestKafkaConfig;
import ru.yandex.practicum.accounts.config.TestSecurityConfig;
import ru.yandex.practicum.accounts.service.TestOutboxConfig;
import ru.yandex.practicum.accounts.entity.OutboxMessage;
import ru.yandex.practicum.accounts.repository.OutboxNotificationRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.cloud.discovery.client.health-indicator.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.consul.discovery.enabled=false",
        "spring.cloud.consul.config.enabled=false",
        "spring.task.scheduling.enabled=false",
        "outbox.scheduler.enabled=false",
        "spring.security.enabled=false",
        "kafka.enabled=false"
    }
)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestExceptionHandlerConfig.class, TestOutboxConfig.class, TestKafkaConfig.class})
class OutboxProcessorIntegrationTest {

    @Autowired
    private OutboxProcessor outboxProcessor;

    @Autowired
    private OutboxNotificationRepository outboxRepository;

    @Autowired
    private OutboxService outboxService;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
    }

    @Test
    void processPendingMessages_shouldProcessPendingMessages() {
        outboxService.saveMessage("test_user", "Test message");
        outboxProcessor.processPendingMessages();
        var messages = outboxRepository.findPendingMessages(10);
        assertThat(messages).isEmpty();
    }

    @Test
    void processPendingMessages_shouldUpdateMessageStatusToSent() {
        OutboxMessage saved = outboxService.saveMessage("test_user", "Test message");
        assertThat(saved.getStatus()).isEqualTo(OutboxMessage.Status.PENDING.getValue());
        outboxProcessor.processPendingMessages();
        OutboxMessage updated = outboxRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OutboxMessage.Status.SENT.getValue());
    }

    @Test
    void processPendingMessages_shouldNotProcessAlreadySentMessages() {
        OutboxMessage saved = outboxService.saveMessage("test_user", "Test message");
        outboxProcessor.processPendingMessages();
        OutboxMessage first = outboxRepository.findById(saved.getId()).orElseThrow();
        assertThat(first.getStatus()).isEqualTo(OutboxMessage.Status.SENT.getValue());
        outboxProcessor.processPendingMessages();
        OutboxMessage second = outboxRepository.findById(first.getId()).orElseThrow();
        assertThat(second.getStatus()).isEqualTo(OutboxMessage.Status.SENT.getValue());
    }
}
