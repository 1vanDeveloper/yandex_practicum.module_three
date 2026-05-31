package ru.yandex.practicum.accounts.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.accounts.dto.AccountIdResponse;
import ru.yandex.practicum.accounts.dto.AccountResponse;
import ru.yandex.practicum.accounts.dto.CreateAccountRequest;
import ru.yandex.practicum.accounts.dto.UpdateAccountRequest;
import ru.yandex.practicum.accounts.entity.Account;
import ru.yandex.practicum.accounts.mapper.AccountMapper;
import ru.yandex.practicum.accounts.repository.AccountRepository;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    @Transactional
    public Mono<AccountIdResponse> createAccount(CreateAccountRequest request) {
        return accountRepository.existsByLogin(request.getLogin())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new AccountAlreadyExistsException(
                                "Account with login '" + request.getLogin() + "' already exists"));
                    }
                    Account account = accountMapper.toEntity(request);
                    return accountRepository.save(account);
                })
                .map(savedAccount -> new AccountIdResponse(savedAccount.getId()));
    }

    @Transactional(readOnly = true)
    public Mono<AccountResponse> getAccountByLogin(String login) {
        return accountRepository.findByLogin(login)
                .map(accountMapper::toResponse)
                .switchIfEmpty(Mono.error(new AccountNotFoundException(
                        "Account with login '" + login + "' not found")));
    }

    @Transactional
    public Mono<AccountResponse> updateAccount(String login, UpdateAccountRequest request) {
        return accountRepository.findByLogin(login)
                .switchIfEmpty(Mono.error(new AccountNotFoundException(
                        "Account with login '" + login + "' not found")))
                .flatMap(account -> {
                    accountMapper.updateEntityFromRequest(request, account);
                    return accountRepository.save(account);
                })
                .map(accountMapper::toResponse);
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
