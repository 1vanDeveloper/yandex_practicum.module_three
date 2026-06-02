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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestExceptionHandlerConfig.class, TestOutboxConfig.class})
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
    void processPendingMessages_shouldProcessPendingMessages() throws Exception {
        // Save message
        outboxService.saveMessage("test_user", "Test message").join();
        
        // Process pending messages
        outboxProcessor.processPendingMessages().join();
        
        // Verify no pending messages remain
        var messages = outboxRepository.findPendingMessages(10);
        assertThat(messages).isEmpty();
    }

    @Test
    void processPendingMessages_shouldUpdateMessageStatusToSent() throws Exception {
        // Save message
        OutboxMessage saved = outboxService.saveMessage("test_user", "Test message").join();
        
        // Verify initial status is PENDING
        assertThat(saved.getStatus()).isEqualTo(OutboxMessage.Status.PENDING.getValue());
        
        // Process and verify status changed to SENT
        outboxProcessor.processPendingMessages().join();
        
        OutboxMessage updated = outboxRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated).isNotNull();
        assertThat(updated.getStatus()).isEqualTo(OutboxMessage.Status.SENT.getValue());
    }

    @Test
    void processPendingMessages_shouldNotProcessAlreadySentMessages() throws Exception {
        // Save message
        OutboxMessage saved = outboxService.saveMessage("test_user", "Test message").join();
        
        // First processing
        outboxProcessor.processPendingMessages().join();
        
        // Verify first processing changed status to SENT
        OutboxMessage first = outboxRepository.findById(saved.getId()).orElseThrow();
        assertThat(first.getStatus()).isEqualTo(OutboxMessage.Status.SENT.getValue());
        
        // Second processing should not change anything
        outboxProcessor.processPendingMessages().join();
        
        OutboxMessage second = outboxRepository.findById(first.getId()).orElseThrow();
        assertThat(second.getStatus()).isEqualTo(OutboxMessage.Status.SENT.getValue());
    }
}
