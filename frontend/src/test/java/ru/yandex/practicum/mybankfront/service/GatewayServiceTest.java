package ru.yandex.practicum.mybankfront.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.mybankfront.client.GatewayClient;
import ru.yandex.practicum.mybankfront.dto.AccountResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GatewayServiceTest {

    @Mock
    private GatewayClient gatewayClient;

    @InjectMocks
    private GatewayService gatewayService;

    private AccountResponse testAccount;

    @BeforeEach
    void setUp() {
        testAccount = AccountResponse.builder()
                .id(1L)
                .login("testuser")
                .firstName("Test")
                .lastName("User")
                .birthDate(LocalDate.of(1990, 1, 1))
                .amount(BigDecimal.valueOf(1000.00))
                .build();
    }

    @Test
    void getAccount_shouldReturnAccountResponse() {
        // Arrange
        when(gatewayClient.getAccount(eq("testuser")))
                .thenReturn(CompletableFuture.completedFuture(testAccount));

        // Act
        CompletableFuture<AccountResponse> result = gatewayService.getAccount("testuser");

        // Assert
        assertNotNull(result);
        assertEquals(testAccount, result.join());
        verify(gatewayClient).getAccount(eq("testuser"));
    }

    @Test
    void updateAccount_shouldReturnUpdatedAccountResponse() {
        // Arrange
        when(gatewayClient.updateAccount(eq("testuser"), eq("Test"), eq("User"), anyString()))
                .thenReturn(CompletableFuture.completedFuture(testAccount));

        // Act
        CompletableFuture<AccountResponse> result = gatewayService.updateAccount(
                "testuser", "Test", "User", "1990-01-01");

        // Assert
        assertNotNull(result);
        assertEquals(testAccount, result.join());
        verify(gatewayClient).updateAccount(eq("testuser"), eq("Test"), eq("User"), anyString());
    }

    @Test
    void processCash_shouldCallGatewayClient() {
        // Arrange
        when(gatewayClient.processCash(eq("testuser"), eq(100), eq("PUT")))
                .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        CompletableFuture<Void> result = gatewayService.processCash("testuser", 100, "PUT");

        // Assert
        assertNotNull(result);
        verify(gatewayClient).processCash(eq("testuser"), eq(100), eq("PUT"));
    }

    @Test
    void processTransfer_shouldCallGatewayClient() {
        // Arrange
        when(gatewayClient.processTransfer(eq("testuser"), eq(500), eq("recipient")))
                .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        CompletableFuture<Void> result = gatewayService.processTransfer("testuser", 500, "recipient");

        // Assert
        assertNotNull(result);
        verify(gatewayClient).processTransfer(eq("testuser"), eq(500), eq("recipient"));
    }
}
