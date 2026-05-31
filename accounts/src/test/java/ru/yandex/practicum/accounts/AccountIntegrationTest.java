package ru.yandex.practicum.accounts;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.yandex.practicum.accounts.dto.CreateAccountRequest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest()
@WithMockUser
class AccountIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void createAccount_shouldCreateAccountAndReturnId() {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .login("integration_test_user")
                .password("$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lqkkO9QS3TzCjH3rS")
                .firstName("Integration")
                .lastName("Test")
                .birthDate(LocalDate.of(1990, 5, 15))
                .amount(BigDecimal.valueOf(1000.00))
                .build();

        webTestClient.post()
                .uri("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").exists()
                .jsonPath("$.id").isNumber();
    }

    @Test
    void createAccount_whenLoginAlreadyExists_shouldReturnConflict() {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .login("duplicate_user")
                .password("$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lqkkO9QS3TzCjH3rS")
                .firstName("First")
                .lastName("User")
                .birthDate(LocalDate.of(1990, 5, 15))
                .amount(BigDecimal.valueOf(1000.00))
                .build();

        // First creation should succeed
        webTestClient.post()
                .uri("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated();

        // Second creation with same login should fail
        webTestClient.post()
                .uri("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void getAccount_whenAccountExists_shouldReturnAccount() {
        CreateAccountRequest createRequest = CreateAccountRequest.builder()
                .login("get_test_user")
                .password("$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lqkkO9QS3TzCjH3rS")
                .firstName("Get")
                .lastName("Test")
                .birthDate(LocalDate.of(1990, 5, 15))
                .amount(BigDecimal.valueOf(1000.00))
                .build();

        // Create account first
        webTestClient.post()
                .uri("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").exists()
                .jsonPath("$.id").isNumber();

        // Get account by login
        webTestClient.get()
                .uri("/accounts/get_test_user")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").exists()
                .jsonPath("$.login").isEqualTo("get_test_user")
                .jsonPath("$.firstName").isEqualTo("Get")
                .jsonPath("$.lastName").isEqualTo("Test");
    }

    @Test
    void getAccount_whenAccountNotFound_shouldReturnNotFound() {
        webTestClient.get()
                .uri("/accounts/nonexistent_user")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void createAccount_whenInvalidRequest_shouldReturnBadRequest() {
        // Missing required fields
        String invalidRequest = "{\"login\": \"ab\"}";

        webTestClient.post()
                .uri("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }
}
