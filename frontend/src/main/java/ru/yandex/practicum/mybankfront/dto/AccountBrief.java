package ru.yandex.practicum.mybankfront.dto;

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
