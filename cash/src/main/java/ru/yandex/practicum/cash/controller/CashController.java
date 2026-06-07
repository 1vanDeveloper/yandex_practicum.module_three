package ru.yandex.practicum.cash.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.cash.dto.DepositRequest;
import ru.yandex.practicum.cash.dto.TransactionIdResponse;
import ru.yandex.practicum.cash.dto.TransactionResponse;
import ru.yandex.practicum.cash.dto.WithdrawRequest;
import ru.yandex.practicum.cash.service.CashService;

import java.util.List;

@RestController
@RequestMapping("/cash")
@RequiredArgsConstructor
@Slf4j
public class CashController {

    private final CashService cashService;

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(@Valid @RequestBody DepositRequest request) {
        log.info("POST /cash/deposit received for login: {}", request.login());
        TransactionResponse response = cashService.deposit(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(@Valid @RequestBody WithdrawRequest request) {
        log.info("POST /cash/withdraw received for login: {}", request.login());
        TransactionResponse response = cashService.withdraw(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions/{login}")
    @PreAuthorize("hasRole('cash:read') or hasRole('admin')")
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @PathVariable String login,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("GET /cash/transactions/{} received", login);
        List<TransactionResponse> transactions = cashService.getTransactionsByLogin(login);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/transactions/{login}/{id}")
    @PreAuthorize("hasRole('cash:read') or hasRole('admin')")
    public ResponseEntity<TransactionResponse> getTransaction(
            @PathVariable Long id,
            @PathVariable String login,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("GET /cash/transactions/{}/{}} received", login, id);
        TransactionResponse response = cashService.getTransactionById(id, login);
        return ResponseEntity.ok(response);
    }
}
