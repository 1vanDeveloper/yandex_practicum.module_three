package ru.yandex.practicum.accounts.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.accounts.dto.AccountIdResponse;
import ru.yandex.practicum.accounts.dto.AccountResponse;
import ru.yandex.practicum.accounts.dto.CreateAccountRequest;
import ru.yandex.practicum.accounts.dto.UpdateAccountRequest;
import ru.yandex.practicum.accounts.entity.Account;
import ru.yandex.practicum.accounts.mapper.AccountMapper;
import ru.yandex.practicum.accounts.repository.AccountRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private OutboxService outboxService;

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
    void createAccount_whenAccountDoesNotExist_shouldCreateAccount() throws Exception {
        Account mappedAccount = Account.builder()
                .login("test_user")
                .password("hashed_password")
                .firstName("Test")
                .lastName("User")
                .birthDate(LocalDate.of(1990, 5, 15))
                .amount(BigDecimal.valueOf(1000.00))
                .build();

        when(accountRepository.existsByLogin("test_user")).thenReturn(false);
        when(accountMapper.toEntity(createRequest)).thenReturn(mappedAccount);
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(outboxService.saveMessage(any(String.class), any(String.class)))
                .thenReturn(CompletableFuture.completedFuture(
                        ru.yandex.practicum.accounts.entity.OutboxMessage.builder()
                                .id(java.util.UUID.randomUUID())
                                .login("test_user")
                                .message("Account created: test_user")
                                .status("PENDING")
                                .retryCount(0)
                                .build()
                ));

        CompletableFuture<AccountIdResponse> result = accountService.createAccount(createRequest);

        assertThat(result.join().getId()).isEqualTo(1L);

        verify(accountRepository).existsByLogin("test_user");
        verify(accountRepository).save(any(Account.class));
        verify(outboxService).saveMessage("test_user", "Account created: test_user");
    }

    @Test
    void createAccount_whenAccountAlreadyExists_shouldThrowException() {
        when(accountRepository.existsByLogin("test_user")).thenReturn(true);

        CompletableFuture<AccountIdResponse> result = accountService.createAccount(createRequest);

        assertThatThrownBy(() -> result.join())
                .hasCauseInstanceOf(AccountService.AccountAlreadyExistsException.class);

        verify(accountRepository).existsByLogin("test_user");
    }

    @Test
    void getAccountByLogin_whenAccountExists_shouldReturnAccount() throws Exception {
        AccountResponse response = AccountResponse.builder()
                .id(1L)
                .login("test_user")
                .firstName("Test")
                .lastName("User")
                .birthDate(LocalDate.of(1990, 5, 15))
                .amount(BigDecimal.valueOf(1000.00))
                .build();

        when(accountRepository.findByLogin("test_user")).thenReturn(java.util.Optional.of(testAccount));
        when(accountMapper.toResponse(testAccount)).thenReturn(response);

        CompletableFuture<AccountResponse> result = accountService.getAccountByLogin("test_user");

        AccountResponse accountResponse = result.join();
        assertThat(accountResponse.getId()).isEqualTo(1L);
        assertThat(accountResponse.getLogin()).isEqualTo("test_user");
        assertThat(accountResponse.getFirstName()).isEqualTo("Test");
    }

    @Test
    void getAccountByLogin_whenAccountNotFound_shouldThrowException() {
        when(accountRepository.findByLogin("nonexistent")).thenReturn(java.util.Optional.empty());

        CompletableFuture<AccountResponse> result = accountService.getAccountByLogin("nonexistent");

        assertThatThrownBy(() -> result.join())
                .hasCauseInstanceOf(AccountService.AccountNotFoundException.class);
    }

    @Test
    void updateAccount_whenAccountExists_shouldUpdateAccount() throws Exception {
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

        when(accountRepository.findByLogin("test_user")).thenReturn(java.util.Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(updatedAccount);
        when(accountMapper.toResponse(updatedAccount)).thenReturn(response);
        when(outboxService.saveMessage(any(String.class), any(String.class)))
                .thenReturn(CompletableFuture.completedFuture(
                        ru.yandex.practicum.accounts.entity.OutboxMessage.builder()
                                .id(java.util.UUID.randomUUID())
                                .login("test_user")
                                .message("Account updated: test_user")
                                .status("PENDING")
                                .retryCount(0)
                                .build()
                ));

        CompletableFuture<AccountResponse> result = accountService.updateAccount("test_user", updateRequest);

        AccountResponse accountResponse = result.join();
        assertThat(accountResponse.getFirstName()).isEqualTo("Updated");
        assertThat(accountResponse.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(2000.00));
    }

    @Test
    void updateAccount_whenAccountNotFound_shouldThrowException() {
        when(accountRepository.findByLogin("nonexistent")).thenReturn(java.util.Optional.empty());

        CompletableFuture<AccountResponse> result = accountService.updateAccount("nonexistent", updateRequest);

        assertThatThrownBy(() -> result.join())
                .hasCauseInstanceOf(AccountService.AccountNotFoundException.class);
    }
}
