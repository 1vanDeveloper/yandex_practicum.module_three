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
        when(gatewayClient.getAccount(anyString()))
                .thenReturn(CompletableFuture.completedFuture(testAccount));

        // Act
        CompletableFuture<AccountResponse> result = gatewayService.getAccount("test-token");

        // Assert
        assertNotNull(result);
        assertEquals(testAccount, result.join());
        verify(gatewayClient).getAccount(anyString());
    }

    @Test
    void updateAccount_shouldReturnUpdatedAccountResponse() {
        // Arrange
        when(gatewayClient.updateAccount(eq("Test"), eq("User"), eq("1990-01-01"), anyString()))
                .thenReturn(CompletableFuture.completedFuture(testAccount));

        // Act
        CompletableFuture<AccountResponse> result = gatewayService.updateAccount(
                "Test", "User", "1990-01-01", "test-token");

        // Assert
        assertNotNull(result);
        assertEquals(testAccount, result.join());
        verify(gatewayClient).updateAccount(eq("Test"), eq("User"), eq("1990-01-01"), anyString());
    }

    @Test
    void processCash_shouldCallGatewayClient() {
        // Arrange
        when(gatewayClient.processCash(eq(100), eq("PUT"), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        CompletableFuture<Void> result = gatewayService.processCash(100, "PUT", "test-token");

        // Assert
        assertNotNull(result);
        verify(gatewayClient).processCash(eq(100), eq("PUT"), anyString());
    }

    @Test
    void processTransfer_shouldCallGatewayClient() {
        // Arrange
        when(gatewayClient.processTransfer(eq(500), eq("recipient"), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        CompletableFuture<Void> result = gatewayService.processTransfer(500, "recipient", "test-token");

        // Assert
        assertNotNull(result);
        verify(gatewayClient).processTransfer(eq(500), eq("recipient"), anyString());
    }
}
