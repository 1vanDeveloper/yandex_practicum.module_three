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

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/transfer")
@RequiredArgsConstructor
@Slf4j
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public CompletableFuture<ResponseEntity<TransferResponse>> createTransfer(
            @Valid @RequestBody TransferRequest request) {
        log.info("POST /transfer received from {} to {}", request.fromLogin(), request.toLogin());
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
