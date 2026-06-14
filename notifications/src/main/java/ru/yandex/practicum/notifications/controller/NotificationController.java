package ru.yandex.practicum.notifications.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.notifications.dto.NotificationRequest;
import ru.yandex.practicum.notifications.service.NotificationService;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/notificate")
    @ResponseStatus(HttpStatus.OK)
    public CompletableFuture<ResponseEntity<Void>> notificate(@Valid @RequestBody NotificationRequest request) {
        return notificationService.logNotification(request)
                .thenApply(v -> ResponseEntity.ok().build());
    }
}
