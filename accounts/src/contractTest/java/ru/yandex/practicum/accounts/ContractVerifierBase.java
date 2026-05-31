package ru.yandex.practicum.accounts;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
@AutoConfigureMessageVerifier
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public abstract class ContractVerifierBase {

    protected WebTestClient webTestClient;

    @BeforeEach
    void setupWebTestClient(ApplicationContext applicationContext) {
        this.webTestClient = WebTestClient.bindToApplicationContext(applicationContext).build();
    }
}
