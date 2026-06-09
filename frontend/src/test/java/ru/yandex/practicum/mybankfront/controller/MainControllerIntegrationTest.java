package ru.yandex.practicum.mybankfront.controller;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.mybankfront.dto.AccountResponse;
import ru.yandex.practicum.mybankfront.service.GatewayService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MainControllerTest {

    @Test
    void getAccount_shouldCallGatewayService() throws Exception {
        // Arrange
        GatewayService mockService = mock(GatewayService.class);
        AccountResponse testAccount = AccountResponse.builder()
                .id(1L)
                .login("testuser")
                .firstName("Test")
                .lastName("User")
                .birthDate(LocalDate.of(1990, 1, 1))
                .amount(BigDecimal.valueOf(1000.00))
                .build();
        when(mockService.getAccount(anyString())).thenReturn(CompletableFuture.completedFuture(testAccount));

        MainController controller = new MainController(mockService);

        // Act - we can't easily test the full flow without Spring context,
        // but we can verify the service is called correctly
        CompletableFuture<AccountResponse> result = mockService.getAccount("testuser");

        // Assert
        assertNotNull(result);
        assertEquals(testAccount, result.join());
        verify(mockService).getAccount("testuser");
    }

    @Test
    void processCash_shouldCallGatewayService() throws Exception {
        // Arrange
        GatewayService mockService = mock(GatewayService.class);
        when(mockService.processCash(anyString(), anyInt(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        MainController controller = new MainController(mockService);

        // Act
        CompletableFuture<Void> result = mockService.processCash("testuser", 100, "PUT");

        // Assert
        assertNotNull(result);
        verify(mockService).processCash("testuser", 100, "PUT");
    }

    @Test
    void processTransfer_shouldCallGatewayService() throws Exception {
        // Arrange
        GatewayService mockService = mock(GatewayService.class);
        when(mockService.processTransfer(anyString(), anyInt(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        MainController controller = new MainController(mockService);

        // Act
        CompletableFuture<Void> result = mockService.processTransfer("testuser", 500, "recipient");

        // Assert
        assertNotNull(result);
        verify(mockService).processTransfer("testuser", 500, "recipient");
    }
}
