package ru.yandex.practicum.accounts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO для ответа на регистрацию пользователя.
 * Не содержит чувствительных данных (пароль).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {
    private Long id;
    private String login;
    private String email;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
}
