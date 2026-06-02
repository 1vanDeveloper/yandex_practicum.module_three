package ru.yandex.practicum.accounts;

import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.accounts.dto.AccountIdResponse;
import ru.yandex.practicum.accounts.dto.AccountResponse;
import ru.yandex.practicum.accounts.service.AccountService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "server.port=0"
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

    @RestControllerAdvice
    static class TestExceptionHandler {
        @ExceptionHandler(AccountService.AccountNotFoundException.class)
        @ResponseStatus(HttpStatus.NOT_FOUND)
        void handleNotFound(AccountService.AccountNotFoundException ex) {}

        @ExceptionHandler(AccountService.AccountAlreadyExistsException.class)
        @ResponseStatus(HttpStatus.CONFLICT)
        void handleAlreadyExists(AccountService.AccountAlreadyExistsException ex) {}
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class MockServiceConfig {
        @Bean
        public AccountService accountService() {
            AccountService mock = Mockito.mock(AccountService.class);

            Mockito.when(mock.getAccountByLogin(Mockito.anyString()))
                    .thenReturn(CompletableFuture.completedFuture(
                            new AccountResponse(
                                    1L,
                                    "test_user",
                                    "Test",
                                    "User",
                                    LocalDate.of(1990, 5, 15),
                                    BigDecimal.valueOf(1000.00))));

            Mockito.when(mock.createAccount(Mockito.any()))
                    .thenReturn(CompletableFuture.completedFuture(
                            new AccountIdResponse(1L)));

            Mockito.when(mock.updateAccount(Mockito.anyString(), Mockito.any()))
                    .thenReturn(CompletableFuture.completedFuture(
                            new AccountResponse(
                                    1L,
                                    "test_user",
                                    "Updated",
                                    "Name",
                                    LocalDate.of(1995, 10, 20),
                                    BigDecimal.valueOf(2000.00))));

            return mock;
        }
    }
}