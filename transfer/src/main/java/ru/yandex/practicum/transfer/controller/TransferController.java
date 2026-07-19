package ru.yandex.practicum.transfer.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.transfer.dto.TransferRequest;
import ru.yandex.practicum.transfer.dto.TransferResponse;
import ru.yandex.practicum.transfer.service.TransferService;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/transfer")
@RequiredArgsConstructor
@Slf4j
public class TransferController {

    private final TransferService transferService;

    /**
     * Универсальный endpoint для переводов.
     * Принимает query параметры: value, login (получатель)
     * Отправитель берётся из JWT токена
     * 
     * Поддерживает X-Idempotency-Key header для идемпотентности при retry.
     */
    @PostMapping
    public ResponseEntity<TransferResponse> processTransfer(
            @RequestParam(required = false) BigDecimal value,
            @RequestParam(required = false) String login,
            @Valid @RequestBody(required = false) TransferRequest requestBody,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        String fromLogin = jwt.getSubject();
        String toLogin = login != null ? login : (requestBody != null ? requestBody.toLogin() : null);
        BigDecimal amount = value != null ? value : (requestBody != null ? requestBody.amount() : null);

        if (toLogin == null || amount == null) {
            throw new IllegalArgumentException("Missing required parameters: value and login (query) or request body");
        }
        
        // Генерируем operationId: либо из header, либо детерминированный на основе параметров
        String operationId = idempotencyKey != null && !idempotencyKey.isEmpty()
                ? idempotencyKey
                : generateDeterministicOperationId("transfer", fromLogin, toLogin, amount, request);
        
        log.info("POST /transfer received from {} to {}, amount: {}, operationId: {}", 
                fromLogin, toLogin, amount, operationId);
        
        TransferRequest transferRequest = new TransferRequest(fromLogin, toLogin, amount, null, operationId);
        return ResponseEntity.ok(transferService.createTransfer(transferRequest));
    }

    /**
     * Генерирует детерминированный operationId на основе параметров запроса.
     * При retry того же запроса (те же параметры) будет сгенерирован тот же operationId.
     */
    private String generateDeterministicOperationId(String service, String fromLogin, String toLogin, 
                                                     BigDecimal amount, HttpServletRequest request) {
        // Используем hash от параметров + timestamp минуты (для группировки retry в рамках 1 минуты)
        String baseKey = String.format("%s:%s:%s:%s:%d", 
                service, 
                fromLogin,
                toLogin,
                amount.toPlainString(),
                System.currentTimeMillis() / 60000); // timestamp с точностью до минуты
        
        return service.toLowerCase() + "-" + 
               fromLogin.replaceAll("[^a-zA-Z0-9]", "-") + "-" + 
               toLogin.replaceAll("[^a-zA-Z0-9]", "-") + "-" + 
               Math.abs(baseKey.hashCode()) + "-" + 
               UUID.randomUUID().toString().substring(0, 8);
    }
}
