package ru.yandex.practicum.transfer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.yandex.practicum.transfer.config.IntegrationTestConfig;
import ru.yandex.practicum.transfer.dto.TransferRequest;
import ru.yandex.practicum.transfer.dto.TransferResponse;
import ru.yandex.practicum.transfer.entity.TransferStatus;
import ru.yandex.practicum.transfer.service.TransferService;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = {IntegrationTestConfig.class})
class TransferControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransferService transferService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        reset(transferService);
    }

    @Test
    void createTransfer_whenValidRequest_returnsOk() throws Exception {
        // Given
        TransferRequest request = new TransferRequest("sender", "receiver", new BigDecimal("100.00"), "Test transfer");
        TransferResponse response = new TransferResponse(
                1L, "sender", "receiver", new BigDecimal("100.00"),
                TransferStatus.COMPLETED, null, null, null
        );

        when(transferService.createTransfer(any(TransferRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        // When & Then
        mockMvc.perform(post("/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(transferService).createTransfer(any(TransferRequest.class));
    }

    @Test
    void getTransferHistory_whenTransfersExist_returnsList() throws Exception {
        // Given
        when(transferService.getTransfersByLogin(anyString()))
                .thenReturn(CompletableFuture.completedFuture(List.of(
                        new TransferResponse(1L, "user1", "user2", new BigDecimal("100.00"),
                                TransferStatus.COMPLETED, null, null, null)
                )));

        // When & Then
        mockMvc.perform(get("/transfer/history/{login}", "test_user"))
                .andExpect(status().isOk());

        verify(transferService).getTransfersByLogin(anyString());
    }

    @Test
    void getTransferHistory_whenNoTransfers_returnsEmptyList() throws Exception {
        // Given
        when(transferService.getTransfersByLogin(anyString()))
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));

        // When & Then
        mockMvc.perform(get("/transfer/history/{login}", "any_user"))
                .andExpect(status().isOk());

        verify(transferService).getTransfersByLogin(anyString());
    }
}
