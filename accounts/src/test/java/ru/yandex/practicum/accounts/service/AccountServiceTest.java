package ru.yandex.practicum.accounts.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.accounts.dto.AccountIdResponse;
import ru.yandex.practicum.accounts.dto.AccountResponse;
import ru.yandex.practicum.accounts.dto.CreateAccountRequest;
import ru.yandex.practicum.accounts.dto.UpdateAccountRequest;
import ru.yandex.practicum.accounts.entity.Account;
import ru.yandex.practicum.accounts.mapper.AccountMapper;
import ru.yandex.practicum.accounts.repository.AccountRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private AccountService accountService;

    private Account testAccount;
    private CreateAccountRequest createRequest;
    private UpdateAccountRequest updateRequest;

    @BeforeEach
    void setUp() {
        testAccount = Account.builder()
                .id(1L)
                .login("test_user")
                .password("hashed_password")
                .firstName("Test")
                .lastName("User")
                .birthDate(LocalDate.of(1990, 5, 15))
                .amount(BigDecimal.valueOf(1000.00))
                .build();

        createRequest = CreateAccountRequest.builder()
                .login("test_user")
                .password("plain_password")
                .firstName("Test")
                .lastName("User")
                .birthDate(LocalDate.of(1990, 5, 15))
                .amount(BigDecimal.valueOf(1000.00))
                .build();

        updateRequest = UpdateAccountRequest.builder()
                .firstName("Updated")
                .lastName("Name")
                .birthDate(LocalDate.of(1995, 10, 20))
                .amount(BigDecimal.valueOf(2000.00))
                .build();
    }

    @Test
    void createAccount_whenAccountDoesNotExist_shouldCreateAccount() {
        Account mappedAccount = Account.builder()
                .login("test_user")
                .password("hashed_password")
                .firstName("Test")
                .lastName("User")
                .birthDate(LocalDate.of(1990, 5, 15))
                .amount(BigDecimal.valueOf(1000.00))
                .build();

        when(accountRepository.existsByLogin("test_user")).thenReturn(Mono.just(false));
        when(accountMapper.toEntity(createRequest)).thenReturn(mappedAccount);
        when(accountRepository.save(any(Account.class))).thenReturn(Mono.just(testAccount));

        Mono<AccountIdResponse> result = accountService.createAccount(createRequest);

        StepVerifier.create(result)
                .assertNext(response -> assertThat(response.getId()).isEqualTo(1L))
                .verifyComplete();

        verify(accountRepository).existsByLogin("test_user");
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void createAccount_whenAccountAlreadyExists_shouldThrowException() {
        when(accountRepository.existsByLogin("test_user")).thenReturn(Mono.just(true));

        Mono<AccountIdResponse> result = accountService.createAccount(createRequest);

        StepVerifier.create(result)
                .expectError(AccountService.AccountAlreadyExistsException.class)
                .verify();

        verify(accountRepository).existsByLogin("test_user");
    }

    @Test
    void getAccountByLogin_whenAccountExists_shouldReturnAccount() {
        AccountResponse response = AccountResponse.builder()
                .id(1L)
                .login("test_user")
                .firstName("Test")
                .lastName("User")
                .birthDate(LocalDate.of(1990, 5, 15))
                .amount(BigDecimal.valueOf(1000.00))
                .build();

        when(accountRepository.findByLogin("test_user")).thenReturn(Mono.just(testAccount));
        when(accountMapper.toResponse(testAccount)).thenReturn(response);

        Mono<AccountResponse> result = accountService.getAccountByLogin("test_user");

        StepVerifier.create(result)
                .assertNext(accountResponse -> {
                    assertThat(accountResponse.getId()).isEqualTo(1L);
                    assertThat(accountResponse.getLogin()).isEqualTo("test_user");
                    assertThat(accountResponse.getFirstName()).isEqualTo("Test");
                })
                .verifyComplete();
    }

    @Test
    void getAccountByLogin_whenAccountNotFound_shouldThrowException() {
        when(accountRepository.findByLogin("nonexistent")).thenReturn(Mono.empty());

        Mono<AccountResponse> result = accountService.getAccountByLogin("nonexistent");

        StepVerifier.create(result)
                .expectError(AccountService.AccountNotFoundException.class)
                .verify();
    }

    @Test
    void updateAccount_whenAccountExists_shouldUpdateAccount() {
        Account updatedAccount = Account.builder()
                .id(1L)
                .login("test_user")
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

        when(accountRepository.findByLogin("test_user")).thenReturn(Mono.just(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(Mono.just(updatedAccount));
        when(accountMapper.toResponse(updatedAccount)).thenReturn(response);

        Mono<AccountResponse> result = accountService.updateAccount("test_user", updateRequest);

        StepVerifier.create(result)
                .assertNext(accountResponse -> {
                    assertThat(accountResponse.getFirstName()).isEqualTo("Updated");
                    assertThat(accountResponse.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(2000.00));
                })
                .verifyComplete();
    }

    @Test
    void updateAccount_whenAccountNotFound_shouldThrowException() {
        when(accountRepository.findByLogin("nonexistent")).thenReturn(Mono.empty());

        Mono<AccountResponse> result = accountService.updateAccount("nonexistent", updateRequest);

        StepVerifier.create(result)
                .expectError(AccountService.AccountNotFoundException.class)
                .verify();
    }
}
