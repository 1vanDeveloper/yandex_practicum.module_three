package ru.yandex.practicum.accounts.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.accounts.dto.AccountIdResponse;
import ru.yandex.practicum.accounts.dto.AccountResponse;
import ru.yandex.practicum.accounts.dto.CreateAccountRequest;
import ru.yandex.practicum.accounts.dto.UpdateAccountRequest;
import ru.yandex.practicum.accounts.entity.Account;
import ru.yandex.practicum.accounts.mapper.AccountMapper;
import ru.yandex.practicum.accounts.repository.AccountRepository;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final OutboxService outboxService;

    @Transactional
    public CompletableFuture<AccountIdResponse> createAccount(CreateAccountRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            if (accountRepository.existsByLogin(request.getLogin())) {
                throw new AccountAlreadyExistsException(
                        "Account with login '" + request.getLogin() + "' already exists");
            }
            Account account = accountMapper.toEntity(request);
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
}
