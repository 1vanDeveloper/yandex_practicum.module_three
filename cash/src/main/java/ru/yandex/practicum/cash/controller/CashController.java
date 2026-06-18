package ru.yandex.practicum.cash.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.cash.dto.DepositRequest;
import ru.yandex.practicum.cash.dto.TransactionResponse;
import ru.yandex.practicum.cash.dto.WithdrawRequest;
import ru.yandex.practicum.cash.service.CashService;

import java.math.BigDecimal;

@RestController
@RequestMapping("/cash")
@RequiredArgsConstructor
@Slf4j
public class CashController {

    private final CashService cashService;

    /**
     * Универсальный endpoint для операций с наличными.
     * Принимает query параметры: value, action, login
     * action: PUT (пополнение), GET (снятие)
     */
    @PostMapping
    public ResponseEntity<TransactionResponse> processCash(
            @RequestParam Integer value,
            @RequestParam String action,
            @RequestParam(required = false) String login,
            @AuthenticationPrincipal Jwt jwt) {

        String userLogin = login != null ? login : jwt.getSubject();
        log.info("POST /cash received for login: {}, action: {}, value: {}", userLogin, action, value);

        if ("PUT".equalsIgnoreCase(action)) {
            DepositRequest request = new DepositRequest(userLogin, BigDecimal.valueOf(value));
            TransactionResponse response = cashService.deposit(request);
            return ResponseEntity.ok(response);
        } else if ("GET".equalsIgnoreCase(action)) {
            WithdrawRequest request = new WithdrawRequest(userLogin, BigDecimal.valueOf(value));
            TransactionResponse response = cashService.withdraw(request);
            return ResponseEntity.ok(response);
        } else {
            throw new IllegalArgumentException("Invalid action: " + action + ". Must be PUT or GET");
        }
    }
}
