package ru.yandex.practicum.accounts.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.accounts.dto.RegisterRequest;
import ru.yandex.practicum.accounts.entity.Account;
import ru.yandex.practicum.accounts.repository.AccountRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Account register(RegisterRequest request) {
        log.info("Registering new user with login: {}", request.getLogin());

        if (accountRepository.findByLogin(request.getLogin()).isPresent()) {
            throw new IllegalArgumentException("User with login '" + request.getLogin() + "' already exists");
        }

        if (accountRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("User with email '" + request.getEmail() + "' already exists");
        }

        Account account = Account.builder()
                .login(request.getLogin())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .birthDate(LocalDate.parse(request.getBirthDate(), DateTimeFormatter.ISO_LOCAL_DATE))
                .amount(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Account saved = accountRepository.save(account);
        log.info("User registered successfully: {}", saved.getLogin());
        return saved;
    }

    @Transactional(readOnly = true)
    public Account getByLogin(String login) {
        return accountRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("User not found with login: " + login));
    }
}
