package ru.yandex.practicum.accounts.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.accounts.dto.JwtTokenResponse;
import ru.yandex.practicum.accounts.dto.LoginRequest;
import ru.yandex.practicum.accounts.dto.RegisterRequest;
import ru.yandex.practicum.accounts.entity.Account;
import ru.yandex.practicum.accounts.repository.AccountRepository;
import ru.yandex.practicum.accounts.util.JwtUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public Account register(RegisterRequest request) {
        log.info("Registering new user with login: {}", request.getLogin());

        if (accountRepository.findByLogin(request.getLogin()).isPresent()) {
            throw new IllegalArgumentException("User with login '" + request.getLogin() + "' already exists");
        }

        if (accountRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("User with email '" + request.getEmail() + "' already exists");
        }

        LocalDate birthDate = LocalDate.parse(request.getBirthDate(), DateTimeFormatter.ISO_LOCAL_DATE);
        if (birthDate.isAfter(LocalDate.now().minusYears(18))) {
            throw new IllegalArgumentException("User must be at least 18 years old");
        }

        Account account = Account.builder()
                .login(request.getLogin())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .birthDate(birthDate)
                .amount(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Account saved = accountRepository.save(account);
        log.info("User registered successfully: {}", saved.getLogin());
        return saved;
    }

    @Transactional(readOnly = true)
    public JwtTokenResponse login(LoginRequest request) {
        log.info("Authenticating user with login: {}", request.getLogin());

        Account account = accountRepository.findByLogin(request.getLogin())
                .orElseThrow(() -> new IllegalArgumentException("Invalid login or password"));

        if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            throw new IllegalArgumentException("Invalid login or password");
        }

        List<String> privileges = jwtUtil.extractPrivilegesFromAccount(account);
        String token = jwtUtil.generateToken(account.getLogin(), privileges);
        log.info("User authenticated successfully: {} with privileges: {}", account.getLogin(), privileges);

        return JwtTokenResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.extractExpiration(token).getTime() - System.currentTimeMillis())
                .login(account.getLogin())
                .privileges(privileges)
                .build();
    }

    @Transactional(readOnly = true)
    public Account getByLogin(String login) {
        return accountRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("User not found with login: " + login));
    }
}
