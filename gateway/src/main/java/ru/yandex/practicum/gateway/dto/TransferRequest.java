package ru.yandex.practicum.gateway.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {

    @NotBlank(message = "From login is required")
    private String fromLogin;

    @NotBlank(message = "To login is required")
    private String toLogin;

    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be positive")
    private Integer amount;
}
