package ru.yandex.practicum.accounts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtTokenResponse {

    private String token;
    private String tokenType;
    private Long expiresIn;
    private String login;
    private List<String> privileges;
}
