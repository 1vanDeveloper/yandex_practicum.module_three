package ru.yandex.practicum.cash.dto;

public record NotificationRequest(
        String login,
        String message
) {
}
