package ru.yandex.practicum.gateway.controller;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.gateway.dto.AccountResponse;
import ru.yandex.practicum.gateway.dto.CashAction;
import ru.yandex.practicum.gateway.service.GatewayService;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GatewayControllerTest {

    @Test
    void getAccount_shouldReturnAccount() throws Exception {
        // Arrange
        GatewayService mockService = mock(GatewayService.class);
        AccountResponse testAccount = AccountResponse.builder()
                .id(1L)
                .login("testuser")
                .firstName("Test")
                .lastName("User")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();
        when(mockService.getAccount(anyString())).thenReturn(CompletableFuture.completedFuture(testAccount));

        GatewayController controller = new GatewayController(mockService);

        // Act
        CompletableFuture<AccountResponse> result = controller.getAccount(
                new MockJwt("testuser")
        );

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.join().getLogin());
        verify(mockService).getAccount("testuser");
    }

    @Test
    void processCash_shouldCallService() throws Exception {
        // Arrange
        GatewayService mockService = mock(GatewayService.class);
        when(mockService.processCash(anyString(), anyInt(), any())).thenReturn(CompletableFuture.completedFuture(null));

        GatewayController controller = new GatewayController(mockService);

        // Act
        CompletableFuture<Void> result = controller.processCash(
                new MockJwt("testuser"),
                100,
                CashAction.PUT
        );

        // Assert
        assertNotNull(result);
        verify(mockService).processCash("testuser", 100, CashAction.PUT);
    }

    @Test
    void processTransfer_shouldCallService() throws Exception {
        // Arrange
        GatewayService mockService = mock(GatewayService.class);
        when(mockService.processTransfer(anyString(), anyString(), anyInt())).thenReturn(CompletableFuture.completedFuture(null));

        GatewayController controller = new GatewayController(mockService);

        // Act
        CompletableFuture<Void> result = controller.processTransfer(
                new MockJwt("fromUser"),
                500,
                "toUser"
        );

        // Assert
        assertNotNull(result);
        verify(mockService).processTransfer("fromUser", "toUser", 500);
    }
}
