package ru.yandex.practicum.cash;

import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import ru.yandex.practicum.cash.client.AccountsClient;
import ru.yandex.practicum.cash.dto.TransactionResponse;
import ru.yandex.practicum.cash.entity.TransactionStatus;
import ru.yandex.practicum.cash.entity.TransactionType;
import ru.yandex.practicum.cash.service.CashService;
import ru.yandex.practicum.cash.service.KafkaNotificationSender;

import java.math.BigDecimal;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "server.port=0",
                "spring.security.enabled=false"
        })
@AutoConfigureMessageVerifier
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, ContractVerifierBase.MockServiceConfig.class})
public abstract class ContractVerifierBase {

    @LocalServerPort
    protected int port;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = this.port;
        RestAssured.config = RestAssuredConfig.config();
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class MockServiceConfig {

        @Bean
        public CashService cashService() {
            CashService mock = Mockito.mock(CashService.class);

            Mockito.when(mock.deposit(Mockito.any()))
                    .thenReturn(new TransactionResponse(
                            1L,
                            "test_user",
                            TransactionType.DEPOSIT,
                            BigDecimal.valueOf(100.00),
                            TransactionStatus.COMPLETED,
                            null,
                            null,
                            null
                    ));

            Mockito.when(mock.withdraw(Mockito.any()))
                    .thenReturn(new TransactionResponse(
                            2L,
                            "test_user",
                            TransactionType.WITHDRAW,
                            BigDecimal.valueOf(50.00),
                            TransactionStatus.COMPLETED,
                            null,
                            null,
                            null
                    ));

            return mock;
        }

        @Bean
        public AccountsClient accountsClient() {
            return Mockito.mock(AccountsClient.class);
        }

        @Bean
        public KafkaNotificationSender kafkaNotificationSender() {
            return Mockito.mock(KafkaNotificationSender.class);
        }
    }
}
