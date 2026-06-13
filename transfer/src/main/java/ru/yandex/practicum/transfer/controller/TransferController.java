package ru.yandex.practicum.transfer.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.transfer.dto.TransferRequest;
import ru.yandex.practicum.transfer.dto.TransferResponse;
import ru.yandex.practicum.transfer.service.TransferService;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
     */
    @PostMapping
    public CompletableFuture<ResponseEntity<TransferResponse>> processTransfer(
            @RequestParam(required = false) Integer value,
            @RequestParam(required = false) String login,
            @Valid @RequestBody(required = false) TransferRequest requestBody,
            @AuthenticationPrincipal Jwt jwt) {
        
        String fromLogin = jwt.getSubject();
        String toLogin = login != null ? login : (requestBody != null ? requestBody.toLogin() : null);
        BigDecimal amount = value != null ? BigDecimal.valueOf(value) : (requestBody != null ? requestBody.amount() : null);
        
        if (toLogin == null || amount == null) {
            throw new IllegalArgumentException("Missing required parameters: value and login (query) or request body");
        }
        
        log.info("POST /transfer received from {} to {}, amount: {}", fromLogin, toLogin, amount);
        TransferRequest request = new TransferRequest(fromLogin, toLogin, amount, null);
        return transferService.createTransfer(request)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('transfer:read') or hasRole('admin')")
    public CompletableFuture<ResponseEntity<TransferResponse>> getTransfer(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("GET /transfer/{} received", id);
        return transferService.getTransferById(id)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/history/{login}")
    @PreAuthorize("hasRole('transfer:read') or hasRole('admin')")
    public CompletableFuture<ResponseEntity<List<TransferResponse>>> getTransferHistory(
            @PathVariable String login,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("GET /transfer/history/{} received", login);
        return transferService.getTransfersByLogin(login)
                .thenApply(ResponseEntity::ok);
    }
}
