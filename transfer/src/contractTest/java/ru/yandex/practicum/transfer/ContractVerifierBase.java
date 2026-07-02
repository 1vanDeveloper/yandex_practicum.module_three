package ru.yandex.practicum.transfer;

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
import ru.yandex.practicum.transfer.client.AccountsClient;
import ru.yandex.practicum.transfer.dto.TransferResponse;
import ru.yandex.practicum.transfer.entity.TransferStatus;
import ru.yandex.practicum.transfer.service.KafkaNotificationSender;
import ru.yandex.practicum.transfer.service.TransferService;

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
        public TransferService transferService() {
            TransferService mock = Mockito.mock(TransferService.class);

            Mockito.when(mock.createTransfer(Mockito.any()))
                    .thenReturn(
                            new TransferResponse(
                                    1L,
                                    "sender_user",
                                    "receiver_user",
                                    BigDecimal.valueOf(100.00),
                                    TransferStatus.COMPLETED,
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
