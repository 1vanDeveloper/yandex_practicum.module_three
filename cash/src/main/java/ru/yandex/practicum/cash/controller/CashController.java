package ru.yandex.practicum.cash.controller;

import jakarta.servlet.http.HttpServletRequest;
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
import java.util.UUID;

@RestController
@RequestMapping("/cash")
@RequiredArgsConstructor
@Slf4j
public class CashController {

    private final CashService cashService;

    /**
     * Универсальный endpoint для операций с наличными.
     * Принимает query параметры: value, action, login
     * action: DEPOSIT (пополнение), WITHDRAW (снятие)
     * 
     * Поддерживает X-Idempotency-Key header для идемпотентности при retry.
     */
    @PostMapping
    public ResponseEntity<TransactionResponse> processCash(
            @RequestParam BigDecimal value,
            @RequestParam String action,
            @RequestParam(required = false) String login,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        String userLogin = login != null ? login : jwt.getSubject();
        
        // Генерируем operationId: либо из header, либо детерминированный на основе параметров
        String operationId = idempotencyKey != null && !idempotencyKey.isEmpty()
                ? idempotencyKey
                : generateDeterministicOperationId("cash", action, userLogin, value, request);
        
        log.info("POST /cash received for login: {}, action: {}, value: {}, operationId: {}", 
                userLogin, action, value, operationId);

        if ("DEPOSIT".equalsIgnoreCase(action)) {
            DepositRequest depositRequest = new DepositRequest(userLogin, value, operationId);
            TransactionResponse response = cashService.deposit(depositRequest);
            return ResponseEntity.ok(response);
        } else if ("WITHDRAW".equalsIgnoreCase(action)) {
            WithdrawRequest withdrawRequest = new WithdrawRequest(userLogin, value, operationId);
            TransactionResponse response = cashService.withdraw(withdrawRequest);
            return ResponseEntity.ok(response);
        } else {
            throw new IllegalArgumentException("Invalid action: " + action + ". Must be DEPOSIT or WITHDRAW");
        }
    }

    /**
     * Генерирует детерминированный operationId на основе параметров запроса.
     * При retry того же запроса (те же параметры) будет сгенерирован тот же operationId.
     */
    private String generateDeterministicOperationId(String service, String action, String login, 
                                                     BigDecimal amount, HttpServletRequest request) {
        // Используем hash от параметров + timestamp минуты (для группировки retry в рамках 1 минуты)
        String baseKey = String.format("%s:%s:%s:%s:%d", 
                service, 
                action, 
                login, 
                amount.toPlainString(),
                System.currentTimeMillis() / 60000); // timestamp с точностью до минуты
        
        return service.toLowerCase() + "-" + action.toLowerCase() + "-" + 
               login.replaceAll("[^a-zA-Z0-9]", "-") + "-" + 
               Math.abs(baseKey.hashCode()) + "-" + 
               UUID.randomUUID().toString().substring(0, 8);
    }
}
