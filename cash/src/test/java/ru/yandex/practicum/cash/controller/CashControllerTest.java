package ru.yandex.practicum.cash.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.yandex.practicum.cash.config.IntegrationTestConfig;
import ru.yandex.practicum.cash.dto.DepositRequest;
import ru.yandex.practicum.cash.dto.TransactionResponse;
import ru.yandex.practicum.cash.dto.WithdrawRequest;
import ru.yandex.practicum.cash.entity.TransactionStatus;
import ru.yandex.practicum.cash.entity.TransactionType;
import ru.yandex.practicum.cash.service.CashService;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Import(IntegrationTestConfig.class)
class CashControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private CashService cashService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        reset(cashService);
    }

    @Test
    void processCash_whenDepositAction_returnsOk() throws Exception {
        // Given
        TransactionResponse response = new TransactionResponse(
                1L, "test_user", TransactionType.DEPOSIT, new BigDecimal("100.00"),
                TransactionStatus.COMPLETED, null, null, null
        );

        when(cashService.deposit(any(DepositRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/cash")
                        .param("value", "100")
                        .param("action", "PUT")
                        .param("login", "test_user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.accountLogin").value("test_user"))
                .andExpect(jsonPath("$.transactionType").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(cashService).deposit(any(DepositRequest.class));
    }

    @Test
    void processCash_whenWithdrawAction_returnsOk() throws Exception {
        // Given
        TransactionResponse response = new TransactionResponse(
                2L, "test_user", TransactionType.WITHDRAW, new BigDecimal("50.00"),
                TransactionStatus.COMPLETED, null, null, null
        );

        when(cashService.withdraw(any(WithdrawRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/cash")
                        .param("value", "50")
                        .param("action", "GET")
                        .param("login", "test_user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.transactionType").value("WITHDRAW"));

        verify(cashService).withdraw(any(WithdrawRequest.class));
    }

    @Test
    void processCash_whenInvalidAction_throwsException() throws Exception {
        // When & Then
        mockMvc.perform(post("/cash")
                        .param("value", "100")
                        .param("action", "INVALID"))
                .andExpect(status().isInternalServerError());
    }
}
