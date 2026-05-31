package ru.yandex.practicum.accounts;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract tests for accounts API.
 * These tests verify that the API contracts defined in src/contractTest/resources/contracts are satisfied.
 */
public class ContractVerifierTest extends ContractVerifierBase {

    private String uniqueLogin() {
        return "test_user_" + Instant.now().getEpochSecond();
    }

    @Test
    public void validate_createAccount() {
        String login = uniqueLogin();
        String requestBody = """
                {
                    "login": "%s",
                    "password": "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lqkkO9QS3TzCjH3rS",
                    "firstName": "Test",
                    "lastName": "User",
                    "birthDate": "1990-05-15",
                    "amount": 1000.00
                }
                """.formatted(login);

        webTestClient.post()
                .uri("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").exists()
                .jsonPath("$.id").isNumber();
    }

    @Test
    public void validate_getAccount() {
        webTestClient.get()
                .uri("/accounts/test_user")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").exists()
                .jsonPath("$.login").isEqualTo("test_user")
                .jsonPath("$.firstName").exists()
                .jsonPath("$.lastName").exists()
                .jsonPath("$.birthDate").exists()
                .jsonPath("$.amount").exists();
    }

    @Test
    public void validate_updateAccount() {
        String requestBody = """
                {
                    "firstName": "Updated",
                    "lastName": "Name",
                    "birthDate": "1995-10-20",
                    "amount": 2000.0
                }
                """;

        webTestClient.patch()
                .uri("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").exists()
                .jsonPath("$.login").isEqualTo("test_user")
                .jsonPath("$.firstName").isEqualTo("Updated")
                .jsonPath("$.lastName").isEqualTo("Name")
                .jsonPath("$.birthDate").isEqualTo("1995-10-20")
                .jsonPath("$.amount").isEqualTo(2000.0);
    }
}
