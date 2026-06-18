package ru.yandex.practicum.transfer.dto;

public record NotificationRequest(
        String login,
        String message
) {
}
