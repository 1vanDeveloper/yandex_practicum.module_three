package ru.yandex.practicum.accounts.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.accounts.dto.AccountBrief;
import ru.yandex.practicum.accounts.dto.AccountIdResponse;
import ru.yandex.practicum.accounts.dto.AccountResponse;
import ru.yandex.practicum.accounts.dto.CreateAccountRequest;
import ru.yandex.practicum.accounts.dto.InternalBalanceRequest;
import ru.yandex.practicum.accounts.dto.UpdateAccountRequest;
import ru.yandex.practicum.accounts.entity.Account;
import ru.yandex.practicum.accounts.exception.AccountAlreadyExistsException;
import ru.yandex.practicum.accounts.exception.AccountNotFoundException;
import ru.yandex.practicum.accounts.exception.InsufficientFundsException;
import ru.yandex.practicum.accounts.mapper.AccountMapper;
import ru.yandex.practicum.accounts.repository.AccountRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final OutboxService outboxService;
    private final PasswordEncoder passwordEncoder;
    private final IdempotencyService idempotencyService;

    @Transactional
    public AccountIdResponse createAccount(CreateAccountRequest request) {
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
        Account savedAccount = accountRepository.save(account);

        outboxService.saveMessage(savedAccount.getLogin(), "Account created: " + savedAccount.getLogin(), "account-created", savedAccount.getLogin());

        return new AccountIdResponse(savedAccount.getId());
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountByLogin(String login) {
        Account account = accountRepository.findByLogin(login)
            .orElseThrow(() -> new AccountNotFoundException(
                    "Account with login '" + login + "' not found"));
        return accountMapper.toResponse(account);
    }

    @Transactional
    public AccountResponse updateAccount(String login, UpdateAccountRequest request) {
        Account account = accountRepository.findByLogin(login)
            .orElseThrow(() -> new AccountNotFoundException(
                    "Account with login '" + login + "' not found"));
        accountMapper.updateEntityFromRequest(request, account);
        Account updatedAccount = accountRepository.save(account);

        outboxService.saveMessage(updatedAccount.getLogin(), "Account updated: " + updatedAccount.getLogin(), "account-updated", updatedAccount.getLogin());

        return accountMapper.toResponse(updatedAccount);
    }

    @Transactional(readOnly = true)
    public List<AccountBrief> getAllAccounts() {
        return accountRepository.findAll().stream()
            .map(account -> new AccountBrief(
                    account.getLogin(),
                    (account.getFirstName() != null ? account.getFirstName() : "") + " " +
                    (account.getLastName() != null ? account.getLastName() : "")
            ))
            .toList();
    }

    /**
     * Внутренний депозит с проверкой идемпотентности.
     * Используется cash сервисом.
     */
    @Transactional
    public Void deposit(InternalBalanceRequest request) {
        // Проверка идемпотентности
        if (idempotencyService.isAlreadyProcessed(request.getOperationId())) {
            log.info("Operation already processed, skipping: operationId={}", request.getOperationId());
            return null;
        }

        log.info("Processing deposit: login={}, amount={}, operationId={}, source={}",
                request.getLogin(), request.getAmount(), request.getOperationId(), request.getSourceService());

        Account account = accountRepository.findByLogin(request.getLogin())
            .orElseThrow(() -> new AccountNotFoundException(
                    "Account with login '" + request.getLogin() + "' not found"));
        account.setAmount(account.getAmount().add(request.getAmount()));
        accountRepository.save(account);

        // Сохраняем operationId
        idempotencyService.markAsCompleted(
                request.getOperationId(),
                "DEPOSIT",
                request.getLogin(),
                request.getSourceService());

        return null;
    }

    /**
     * Внутреннее списание с проверкой идемпотентности.
     * Используется cash сервисом.
     */
    @Transactional
    public Void withdraw(InternalBalanceRequest request) {
        // Проверка идемпотентности
        if (idempotencyService.isAlreadyProcessed(request.getOperationId())) {
            log.info("Operation already processed, skipping: operationId={}", request.getOperationId());
            return null;
        }

        log.info("Processing withdraw: login={}, amount={}, operationId={}, source={}",
                request.getLogin(), request.getAmount(), request.getOperationId(), request.getSourceService());

        Account account = accountRepository.findByLogin(request.getLogin())
            .orElseThrow(() -> new AccountNotFoundException(
                    "Account with login '" + request.getLogin() + "' not found"));
        if (account.getAmount().compareTo(request.getAmount()) < 0) {
            // Сохраняем как FAILED
            idempotencyService.markAsFailed(
                    request.getOperationId(),
                    "WITHDRAW",
                    request.getLogin(),
                    request.getSourceService());
            throw new InsufficientFundsException(
                    "Insufficient funds for account '" + request.getLogin() + "'. Required: " + request.getAmount() + ", Available: " + account.getAmount());
        }
        account.setAmount(account.getAmount().subtract(request.getAmount()));
        accountRepository.save(account);

        // Сохраняем operationId
        idempotencyService.markAsCompleted(
                request.getOperationId(),
                "WITHDRAW",
                request.getLogin(),
                request.getSourceService());

        return null;
    }

    /**
     * Внутренний дебет с проверкой идемпотентности.
     * Используется transfer сервисом.
     */
    @Transactional
    public Void debit(InternalBalanceRequest request) {
        // Проверка идемпотентности
        if (idempotencyService.isAlreadyProcessed(request.getOperationId())) {
            log.info("Operation already processed, skipping: operationId={}", request.getOperationId());
            return null;
        }

        log.info("Processing debit: login={}, amount={}, operationId={}, source={}",
                request.getLogin(), request.getAmount(), request.getOperationId(), request.getSourceService());

        Account account = accountRepository.findByLogin(request.getLogin())
            .orElseThrow(() -> new AccountNotFoundException(
                    "Account with login '" + request.getLogin() + "' not found"));
        if (account.getAmount().compareTo(request.getAmount()) < 0) {
            // Сохраняем как FAILED
            idempotencyService.markAsFailed(
                    request.getOperationId(),
                    "DEBIT",
                    request.getLogin(),
                    request.getSourceService());
            throw new InsufficientFundsException(
                    "Insufficient funds for account '" + request.getLogin() + "'. Required: " + request.getAmount() + ", Available: " + account.getAmount());
        }
        account.setAmount(account.getAmount().subtract(request.getAmount()));
        accountRepository.save(account);

        // Сохраняем operationId
        idempotencyService.markAsCompleted(
                request.getOperationId(),
                "DEBIT",
                request.getLogin(),
                request.getSourceService());

        return null;
    }

    /**
     * Внутренний кредит с проверкой идемпотентности.
     * Используется transfer сервисом.
     */
    @Transactional
    public Void credit(InternalBalanceRequest request) {
        // Проверка идемпотентности
        if (idempotencyService.isAlreadyProcessed(request.getOperationId())) {
            log.info("Operation already processed, skipping: operationId={}", request.getOperationId());
            return null;
        }

        log.info("Processing credit: login={}, amount={}, operationId={}, source={}",
                request.getLogin(), request.getAmount(), request.getOperationId(), request.getSourceService());

        Account account = accountRepository.findByLogin(request.getLogin())
            .orElseThrow(() -> new AccountNotFoundException(
                    "Account with login '" + request.getLogin() + "' not found"));
        account.setAmount(account.getAmount().add(request.getAmount()));
        accountRepository.save(account);

        // Сохраняем operationId
        idempotencyService.markAsCompleted(
                request.getOperationId(),
                "CREDIT",
                request.getLogin(),
                request.getSourceService());

        return null;
    }
}
