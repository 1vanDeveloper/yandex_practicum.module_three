package ru.yandex.practicum.mybankfront.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountResponse {

    private Long id;
    private String login;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private BigDecimal amount;
}
