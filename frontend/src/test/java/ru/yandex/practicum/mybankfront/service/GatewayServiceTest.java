package ru.yandex.practicum.mybankfront.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.mybankfront.client.GatewayClient;
import ru.yandex.practicum.mybankfront.dto.AccountResponse;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GatewayServiceTest {

    @Mock
    private GatewayClient gatewayClient;

    @InjectMocks
    private GatewayService gatewayService;

    private AccountResponse testAccount;

    @BeforeEach
    void setUp() throws Exception {
        testAccount = AccountResponse.builder()
                .id(1L)
                .login("testuser")
                .firstName("Test")
                .lastName("User")
                .birthDate(LocalDate.of(1990, 1, 1))
                .amount(BigDecimal.valueOf(1000.00))
                .build();

        // Set gatewayUrl field via reflection since @Value is not processed in unit tests
        Field field = gatewayService.getClass().getDeclaredField("gatewayUrl");
        field.setAccessible(true);
        field.set(gatewayService, "http://localhost:8086");
    }

    @Test
    void getAccount_shouldReturnAccountResponse() {
        // Arrange
        when(gatewayClient.getAccount(anyString(), eq("testuser")))
                .thenReturn(CompletableFuture.completedFuture(testAccount));

        // Act
        CompletableFuture<AccountResponse> result = gatewayService.getAccount("testuser");

        // Assert
        assertNotNull(result);
        assertEquals(testAccount, result.join());
        verify(gatewayClient).getAccount(anyString(), eq("testuser"));
    }

    @Test
    void updateAccount_shouldReturnUpdatedAccountResponse() {
        // Arrange
        when(gatewayClient.updateAccount(anyString(), eq("Test"), eq("User"), anyString()))
                .thenReturn(CompletableFuture.completedFuture(testAccount));

        // Act
        CompletableFuture<AccountResponse> result = gatewayService.updateAccount(
                "testuser", "Test", "User", "1990-01-01");

        // Assert
        assertNotNull(result);
        assertEquals(testAccount, result.join());
        verify(gatewayClient).updateAccount(anyString(), eq("Test"), eq("User"), anyString());
    }

    @Test
    void processCash_shouldCallGatewayClient() {
        // Arrange
        when(gatewayClient.processCash(anyString(), eq(100), eq("PUT")))
                .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        CompletableFuture<Void> result = gatewayService.processCash("testuser", 100, "PUT");

        // Assert
        assertNotNull(result);
        verify(gatewayClient).processCash(anyString(), eq(100), eq("PUT"));
    }

    @Test
    void processTransfer_shouldCallGatewayClient() {
        // Arrange
        when(gatewayClient.processTransfer(anyString(), eq(500), eq("recipient")))
                .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        CompletableFuture<Void> result = gatewayService.processTransfer("testuser", 500, "recipient");

        // Assert
        assertNotNull(result);
        verify(gatewayClient).processTransfer(anyString(), eq(500), eq("recipient"));
    }
}
