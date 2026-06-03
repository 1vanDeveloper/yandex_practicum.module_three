package ru.yandex.practicum.accounts.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import ru.yandex.practicum.accounts.config.TestExceptionHandlerConfig;
import ru.yandex.practicum.accounts.config.TestSecurityConfig;
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
        "spring.task.scheduling.enabled=true",
        "outbox.scheduler.enabled=true",
        "spring.security.enabled=false"
    }
)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestExceptionHandlerConfig.class, TestOutboxConfig.class})
class OutboxSchedulerIntegrationTest {

    @Autowired
    private OutboxScheduler outboxScheduler;

    @Autowired
    private OutboxNotificationRepository outboxRepository;

    @Autowired
    private OutboxService outboxService;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
    }

    @Test
    void processOutboxMessages_shouldProcessPendingMessages() throws Exception {
        // Save message
        OutboxMessage saved = outboxService.saveMessage("test_user", "Test message").join();
        assertThat(saved.getStatus()).isEqualTo(OutboxMessage.Status.PENDING.getValue());

        // Run scheduler
        outboxScheduler.processOutboxMessages();

        // Give scheduler time to process
        Thread.sleep(500);

        // Verify no pending messages remain
        var messages = outboxRepository.findPendingMessages(10);
        assertThat(messages).isEmpty();
    }

    @Test
    void processOutboxMessages_shouldUpdateMessageStatus() throws Exception {
        // Save message
        OutboxMessage saved = outboxService.saveMessage("test_user", "Test message").join();

        // Verify initial status is PENDING
        assertThat(saved.getStatus()).isEqualTo(OutboxMessage.Status.PENDING.getValue());

        // Run scheduler
        outboxScheduler.processOutboxMessages();

        // Give scheduler time to process
        Thread.sleep(500);

        // Verify status changed to SENT
        OutboxMessage updated = outboxRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated).isNotNull();
        assertThat(updated.getStatus()).isEqualTo(OutboxMessage.Status.SENT.getValue());
    }
}
