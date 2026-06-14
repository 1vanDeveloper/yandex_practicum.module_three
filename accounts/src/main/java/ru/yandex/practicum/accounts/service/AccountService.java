package ru.yandex.practicum.accounts.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.accounts.dto.AccountBrief;
import ru.yandex.practicum.accounts.dto.AccountIdResponse;
import ru.yandex.practicum.accounts.dto.AccountResponse;
import ru.yandex.practicum.accounts.dto.CreateAccountRequest;
import ru.yandex.practicum.accounts.dto.UpdateAccountRequest;
import ru.yandex.practicum.accounts.entity.Account;
import ru.yandex.practicum.accounts.mapper.AccountMapper;
import ru.yandex.practicum.accounts.repository.AccountRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final OutboxService outboxService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public CompletableFuture<AccountIdResponse> createAccount(CreateAccountRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            if (accountRepository.existsByLogin(request.getLogin())) {
                throw new AccountAlreadyExistsException(
                        "Account with login '" + request.getLogin() + "' already exists");
            }
            if (accountRepository.existsByEmail(request.getEmail())) {
                throw new AccountAlreadyExistsException(
                        "Account with email '" + request.getEmail() + "' already exists");
            }
            Account account = accountMapper.toEntity(request);
            account.setPassword(passwordEncoder.encode(account.getPassword()));
            return accountRepository.save(account);
        }).thenCompose(savedAccount ->
            outboxService.saveMessage(savedAccount.getLogin(), "Account created: " + savedAccount.getLogin())
                .thenApply(v -> new AccountIdResponse(savedAccount.getId()))
        );
    }

    @Transactional(readOnly = true)
    public CompletableFuture<AccountResponse> getAccountByLogin(String login) {
        return CompletableFuture.supplyAsync(() -> {
            Account account = accountRepository.findByLogin(login)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account with login '" + login + "' not found"));
            return accountMapper.toResponse(account);
        });
    }

    @Transactional
    public CompletableFuture<AccountResponse> updateAccount(String login, UpdateAccountRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            Account account = accountRepository.findByLogin(login)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account with login '" + login + "' not found"));
            accountMapper.updateEntityFromRequest(request, account);
            return accountRepository.save(account);
        }).thenCompose(updatedAccount ->
            outboxService.saveMessage(updatedAccount.getLogin(), "Account updated: " + updatedAccount.getLogin())
                .thenApply(v -> accountMapper.toResponse(updatedAccount))
        );
    }

    @Transactional(readOnly = true)
    public CompletableFuture<List<AccountBrief>> getAllAccounts() {
        return CompletableFuture.supplyAsync(() ->
            accountRepository.findAll().stream()
                .map(account -> new AccountBrief(
                        account.getLogin(),
                        (account.getFirstName() != null ? account.getFirstName() : "") + " " +
                        (account.getLastName() != null ? account.getLastName() : "")
                ))
                .toList()
        );
    }

    @Transactional
    public CompletableFuture<Void> deposit(String login, BigDecimal amount) {
        return CompletableFuture.runAsync(() -> {
            Account account = accountRepository.findByLogin(login)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account with login '" + login + "' not found"));
            account.setAmount(account.getAmount().add(amount));
            accountRepository.save(account);
        });
    }

    @Transactional
    public CompletableFuture<Void> withdraw(String login, BigDecimal amount) {
        return CompletableFuture.runAsync(() -> {
            Account account = accountRepository.findByLogin(login)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account with login '" + login + "' not found"));
            if (account.getAmount().compareTo(amount) < 0) {
                throw new InsufficientFundsException(
                        "Insufficient funds for account '" + login + "'. Required: " + amount + ", Available: " + account.getAmount());
            }
            account.setAmount(account.getAmount().subtract(amount));
            accountRepository.save(account);
        });
    }

    @Transactional
    public CompletableFuture<Void> debit(String login, BigDecimal amount) {
        return CompletableFuture.runAsync(() -> {
            Account account = accountRepository.findByLogin(login)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account with login '" + login + "' not found"));
            if (account.getAmount().compareTo(amount) < 0) {
                throw new InsufficientFundsException(
                        "Insufficient funds for account '" + login + "'. Required: " + amount + ", Available: " + account.getAmount());
            }
            account.setAmount(account.getAmount().subtract(amount));
            accountRepository.save(account);
        });
    }

    @Transactional
    public CompletableFuture<Void> credit(String login, BigDecimal amount) {
        return CompletableFuture.runAsync(() -> {
            Account account = accountRepository.findByLogin(login)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account with login '" + login + "' not found"));
            account.setAmount(account.getAmount().add(amount));
            accountRepository.save(account);
        });
    }

    public static class AccountNotFoundException extends RuntimeException {
        public AccountNotFoundException(String message) {
            super(message);
        }
    }

    public static class AccountAlreadyExistsException extends RuntimeException {
        public AccountAlreadyExistsException(String message) {
            super(message);
        }
    }

    public static class InsufficientFundsException extends RuntimeException {
        public InsufficientFundsException(String message) {
            super(message);
        }
    }
}
