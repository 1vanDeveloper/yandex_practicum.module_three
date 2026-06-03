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
        "spring.task.scheduling.enabled=false",
        "outbox.scheduler.enabled=false",
        "spring.security.enabled=false"
    }
)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestExceptionHandlerConfig.class, TestOutboxConfig.class})
class OutboxServiceIntegrationTest {

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private OutboxNotificationRepository outboxRepository;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
    }

    @Test
    void saveMessage_shouldSaveMessageToDatabase() {
        String login = "test_user";
        String message = "Test notification message";

        // Save message and verify it returns correct data
        OutboxMessage savedMessage = outboxService.saveMessage(login, message).join();

        assertThat(savedMessage.getId()).isNotNull();
        assertThat(savedMessage.getLogin()).isEqualTo(login);
        assertThat(savedMessage.getMessage()).isEqualTo(message);
        assertThat(savedMessage.getStatus()).isEqualTo(OutboxMessage.Status.PENDING.getValue());
        assertThat(savedMessage.getRetryCount()).isEqualTo(0);
        assertThat(savedMessage.getCreatedAt()).isNotNull();
        assertThat(savedMessage.getUpdatedAt()).isNotNull();
    }

    @Test
    void saveMessage_shouldSetCorrectTimestamps() {
        OutboxMessage savedMessage = outboxService.saveMessage("user", "message").join();

        assertThat(savedMessage.getCreatedAt()).isNotNull();
        assertThat(savedMessage.getUpdatedAt()).isNotNull();
    }

    @Test
    void findPendingMessages_shouldReturnOnlyPendingMessages() {
        // Save two messages and verify findPendingMessages returns them correctly
        outboxService.saveMessage("user1", "message1").join();
        outboxService.saveMessage("user2", "message2").join();

        var messages = outboxRepository.findPendingMessages(10);

        assertThat(messages).hasSize(2);
        assertThat(messages).extracting("login")
                .containsExactlyInAnyOrder("user1", "user2");
        assertThat(messages).allMatch(m ->
            m.getStatus().equals(OutboxMessage.Status.PENDING.getValue()));
    }
}
