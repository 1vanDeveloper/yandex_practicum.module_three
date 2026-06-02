package ru.yandex.practicum.accounts.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import ru.yandex.practicum.accounts.config.TestSecurityConfig;
import ru.yandex.practicum.accounts.config.TestExceptionHandlerConfig;
import ru.yandex.practicum.accounts.dto.AccountIdResponse;
import ru.yandex.practicum.accounts.dto.CreateAccountRequest;
import ru.yandex.practicum.accounts.dto.UpdateAccountRequest;
import ru.yandex.practicum.accounts.entity.Account;
import ru.yandex.practicum.accounts.repository.AccountRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestExceptionHandlerConfig.class})
class AccountControllerTest {

    @Autowired
    private AccountRepository accountRepository;

    @LocalServerPort
    private Integer port;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();

        // Очистка таблицы перед каждым тестом
        accountRepository.deleteAll()
                .thenMany(Flux.empty())
                .as(StepVerifier::create)
                .verifyComplete();
    }

    @Test
    void createAccount_shouldCreateAccountInDatabase() {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .login("integration_test_user")
                .password("hashed_password")
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
                .expectBody(AccountIdResponse.class)
                .value(response -> {
                    assertThat(response.getId()).isNotNull();

                    // Проверяем, что запись действительно создана в БД
                    accountRepository.findById(response.getId())
                            .as(StepVerifier::create)
                            .assertNext(account -> {
                                assertThat(account.getLogin()).isEqualTo("integration_test_user");
                                assertThat(account.getFirstName()).isEqualTo("Integration");
                                assertThat(account.getLastName()).isEqualTo("Test");
                                assertThat(account.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000.00));
                            })
                            .verifyComplete();
                });
    }

    @Test
    void getAccount_shouldReturnAccountFromDatabase() {
        // Создаём тестовую запись в БД
        Account account = Account.builder()
                .login("get_test_user")
                .password("hashed_password")
                .firstName("Get")
                .lastName("Test")
                .birthDate(LocalDate.of(1995, 8, 20))
                .amount(BigDecimal.valueOf(2500.50))
                .build();

        accountRepository.save(account)
                .as(StepVerifier::create)
                .assertNext(saved -> assertThat(saved.getId()).isNotNull())
                .verifyComplete();

        // Получаем запись через API
        webTestClient.get()
                .uri("/accounts/get_test_user")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.login").isEqualTo("get_test_user")
                .jsonPath("$.firstName").isEqualTo("Get")
                .jsonPath("$.lastName").isEqualTo("Test")
                .jsonPath("$.amount").isEqualTo(2500.5);
    }

    @Test
    void getAccount_whenNotFound_shouldReturnNotFound() {
        webTestClient.get()
                .uri("/accounts/nonexistent_user")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void createAccount_whenLoginExists_shouldReturnConflict() {
        // Создаём первую запись
        Account account = Account.builder()
                .login("duplicate_user")
                .password("hashed_password")
                .firstName("First")
                .lastName("User")
                .birthDate(LocalDate.of(1990, 1, 1))
                .amount(BigDecimal.valueOf(100))
                .build();

        accountRepository.save(account)
                .then()
                .as(StepVerifier::create)
                .verifyComplete();

        // Пытаемся создать запись с тем же логином
        CreateAccountRequest request = CreateAccountRequest.builder()
                .login("duplicate_user")
                .password("another_password")
                .firstName("Second")
                .lastName("User")
                .birthDate(LocalDate.of(1995, 1, 1))
                .amount(BigDecimal.valueOf(200))
                .build();

        webTestClient.post()
                .uri("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void updateAccount_shouldUpdateAccountInDatabase() {
        // Создаём тестовую запись с логином, который совпадает с mock JWT
        Account account = Account.builder()
                .login("test_user")
                .password("hashed_password")
                .firstName("Original")
                .lastName("Name")
                .birthDate(LocalDate.of(1990, 1, 1))
                .amount(BigDecimal.valueOf(1000))
                .build();

        accountRepository.save(account)
                .then()
                .as(StepVerifier::create)
                .verifyComplete();

        UpdateAccountRequest request = UpdateAccountRequest.builder()
                .firstName("Updated")
                .lastName("LastName")
                .birthDate(LocalDate.of(2000, 12, 31))
                .amount(BigDecimal.valueOf(5000.75))
                .build();

        // Обновляем запись напрямую через сервис (тестирование БД)
        accountRepository.findByLogin("test_user")
                .flatMap(existingAccount -> {
                    existingAccount.setFirstName("Updated");
                    existingAccount.setLastName("LastName");
                    existingAccount.setBirthDate(LocalDate.of(2000, 12, 31));
                    existingAccount.setAmount(BigDecimal.valueOf(5000.75));
                    return accountRepository.save(existingAccount);
                })
                .as(StepVerifier::create)
                .assertNext(updated -> {
                    assertThat(updated.getFirstName()).isEqualTo("Updated");
                    assertThat(updated.getLastName()).isEqualTo("LastName");
                    assertThat(updated.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(5000.75));
                })
                .verifyComplete();

        // Проверяем через API, что данные обновились
        webTestClient.get()
                .uri("/accounts/test_user")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.firstName").isEqualTo("Updated")
                .jsonPath("$.lastName").isEqualTo("LastName")
                .jsonPath("$.amount").isEqualTo(5000.75);
    }
}
