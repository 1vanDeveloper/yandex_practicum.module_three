package ru.yandex.practicum.frontend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountBrief {
    private String login;
    private String name;
}
