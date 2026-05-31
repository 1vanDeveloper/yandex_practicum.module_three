package ru.yandex.practicum.accounts.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.accounts.dto.AccountIdResponse;
import ru.yandex.practicum.accounts.dto.AccountResponse;
import ru.yandex.practicum.accounts.dto.CreateAccountRequest;
import ru.yandex.practicum.accounts.dto.UpdateAccountRequest;
import ru.yandex.practicum.accounts.service.AccountService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountController accountController;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient
                .bindToController(accountController)
                .controllerAdvice(new TestExceptionHandler())
                .build();
    }

    @RestControllerAdvice
    static class TestExceptionHandler {

        @ExceptionHandler(AccountService.AccountNotFoundException.class)
        @ResponseStatus(HttpStatus.NOT_FOUND)
        void handleNotFound(AccountService.AccountNotFoundException ex) {
        }

        @ExceptionHandler(AccountService.AccountAlreadyExistsException.class)
        @ResponseStatus(HttpStatus.CONFLICT)
        void handleAlreadyExists(AccountService.AccountAlreadyExistsException ex) {
        }
    }

    @Test
    void createAccount_shouldReturnCreatedAccountId() {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .login("test_user")
                .password("hashed_password")
                .firstName("Test")
                .lastName("User")
                .birthDate(LocalDate.of(1990, 5, 15))
                .amount(BigDecimal.valueOf(1000.00))
                .build();

        AccountIdResponse response = new AccountIdResponse(1L);

        when(accountService.createAccount(any(CreateAccountRequest.class)))
                .thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1);
    }

    @Test
    void getAccount_shouldReturnAccount() {
        AccountResponse response = AccountResponse.builder()
                .id(1L)
                .login("test_user")
                .firstName("Test")
                .lastName("User")
                .birthDate(LocalDate.of(1990, 5, 15))
                .amount(BigDecimal.valueOf(1000.00))
                .build();

        when(accountService.getAccountByLogin("test_user"))
                .thenReturn(Mono.just(response));

        webTestClient.get()
                .uri("/accounts/test_user")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.login").isEqualTo("test_user")
                .jsonPath("$.firstName").isEqualTo("Test");
    }

    @Test
    void getAccount_whenNotFound_shouldReturnNotFound() {
        when(accountService.getAccountByLogin("nonexistent"))
                .thenReturn(Mono.error(new AccountService.AccountNotFoundException("Account not found")));

        webTestClient.get()
                .uri("/accounts/nonexistent")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void updateAccount_shouldReturnUpdatedAccount() {
        // Note: This test verifies the service call is made correctly.
        // The JWT authentication is tested at the integration level.
        UpdateAccountRequest request = UpdateAccountRequest.builder()
                .firstName("Updated")
                .lastName("Name")
                .birthDate(LocalDate.of(1995, 10, 20))
                .amount(BigDecimal.valueOf(2000.00))
                .build();

        AccountResponse response = AccountResponse.builder()
                .id(1L)
                .login("test_user")
                .firstName("Updated")
                .lastName("Name")
                .birthDate(LocalDate.of(1995, 10, 20))
                .amount(BigDecimal.valueOf(2000.00))
                .build();

        when(accountService.updateAccount(eq("test_user"), any(UpdateAccountRequest.class)))
                .thenReturn(Mono.just(response));

        // Create a mock JWT with the required claim
        Jwt mockJwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Collections.singletonMap("alg", "none"),
                Collections.singletonMap("preferred_username", "test_user")
        );

        // Verify the service method is called correctly (JWT authentication is tested at integration level)
        Mono<AccountResponse> result = accountController.updateAccount(mockJwt, request);

        StepVerifier.create(result)
                .assertNext(accountResponse -> {
                    assertThat(accountResponse.getId()).isEqualTo(1L);
                    assertThat(accountResponse.getLogin()).isEqualTo("test_user");
                    assertThat(accountResponse.getFirstName()).isEqualTo("Updated");
                })
                .verifyComplete();

        verify(accountService).updateAccount(eq("test_user"), any(UpdateAccountRequest.class));
    }
}
