package ru.yandex.practicum.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Контроллер для обработки fallback'ов Circuit Breaker.
 */
@RestController
@Slf4j
public class FallbackController {

    @GetMapping("/fallback/accounts")
    public Mono<ResponseEntity<String>> accountsFallback() {
        log.error("Accounts service fallback triggered");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body("Accounts service is temporarily unavailable"));
    }

    @GetMapping("/fallback/cash")
    public Mono<ResponseEntity<String>> cashFallback() {
        log.error("Cash service fallback triggered");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body("Cash service is temporarily unavailable"));
    }

    @GetMapping("/fallback/transfer")
    public Mono<ResponseEntity<String>> transferFallback() {
        log.error("Transfer service fallback triggered");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body("Transfer service is temporarily unavailable"));
    }
}
