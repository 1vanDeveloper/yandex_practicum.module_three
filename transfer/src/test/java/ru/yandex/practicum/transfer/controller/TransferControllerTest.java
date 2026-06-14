package ru.yandex.practicum.transfer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.yandex.practicum.transfer.config.IntegrationTestConfig;
import ru.yandex.practicum.transfer.dto.TransferRequest;
import ru.yandex.practicum.transfer.dto.TransferResponse;
import ru.yandex.practicum.transfer.entity.TransferStatus;
import ru.yandex.practicum.transfer.service.TransferService;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = {IntegrationTestConfig.class})
class TransferControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private FilterChainProxy filterChainProxy;

    @MockitoBean
    private TransferService transferService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(filterChainProxy)
                .build();
        reset(transferService);
    }

    @Test
    void processTransfer_whenValidRequest_returnsOk() throws Exception {
        // Given
        TransferResponse response = new TransferResponse(
                1L, "sender", "receiver", new BigDecimal("100.00"),
                TransferStatus.COMPLETED, null, null, null
        );

        when(transferService.createTransfer(any(TransferRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        // When & Then
        mockMvc.perform(post("/transfer")
                        .param("value", "100")
                        .param("login", "receiver"))
                .andExpect(status().isOk());

        verify(transferService).createTransfer(any(TransferRequest.class));
    }
}
