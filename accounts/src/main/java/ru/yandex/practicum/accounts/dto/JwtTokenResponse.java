package ru.yandex.practicum.accounts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtTokenResponse {

    private String token;
    private String tokenType;
    private Long expiresIn;
    private String login;
}
