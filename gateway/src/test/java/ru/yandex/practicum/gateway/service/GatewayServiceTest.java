package ru.yandex.practicum.gateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.gateway.client.AccountsClient;
import ru.yandex.practicum.gateway.client.CashClient;
import ru.yandex.practicum.gateway.client.TransferClient;
import ru.yandex.practicum.gateway.dto.AccountResponse;
import ru.yandex.practicum.gateway.dto.CashAction;
import ru.yandex.practicum.gateway.dto.UpdateAccountRequest;

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
    private AccountsClient accountsClient;

    @Mock
    private CashClient cashClient;

    @Mock
    private TransferClient transferClient;

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

        // Set URL fields via reflection since @Value is not processed in unit tests
        setField(gatewayService, "accountsUrl", "http://localhost:8082");
        setField(gatewayService, "cashUrl", "http://localhost:8084");
        setField(gatewayService, "transferUrl", "http://localhost:8085");
    }

    private void setField(Object target, String fieldName, String value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void getAccount_shouldReturnAccountResponse() {
        // Arrange
        when(accountsClient.getAccount(anyString(), eq("testuser")))
                .thenReturn(CompletableFuture.completedFuture(testAccount));

        // Act
        CompletableFuture<AccountResponse> result = gatewayService.getAccount("testuser");

        // Assert
        assertNotNull(result);
        assertEquals(testAccount, result.join());
        verify(accountsClient).getAccount(anyString(), eq("testuser"));
    }

    @Test
    void updateAccount_shouldReturnUpdatedAccountResponse() {
        // Arrange
        UpdateAccountRequest request = UpdateAccountRequest.builder()
                .firstName("Updated")
                .lastName("Name")
                .birthDate(LocalDate.of(1995, 5, 5))
                .build();

        when(accountsClient.updateAccount(anyString(), eq(request)))
                .thenReturn(CompletableFuture.completedFuture(testAccount));

        // Act
        CompletableFuture<AccountResponse> result = gatewayService.updateAccount("testuser", request);

        // Assert
        assertNotNull(result);
        assertEquals(testAccount, result.join());
        verify(accountsClient).updateAccount(anyString(), eq(request));
    }

    @Test
    void processCash_withPutAction_shouldCallDeposit() {
        // Arrange
        when(cashClient.deposit(anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        CompletableFuture<Void> result = gatewayService.processCash("testuser", 100, CashAction.PUT);

        // Assert
        assertNotNull(result);
        verify(cashClient).deposit(anyString(), any());
        verifyNoInteractions(transferClient);
    }

    @Test
    void processCash_withGetAction_shouldCallWithdraw() {
        // Arrange
        when(cashClient.withdraw(anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        CompletableFuture<Void> result = gatewayService.processCash("testuser", 50, CashAction.GET);

        // Assert
        assertNotNull(result);
        verify(cashClient).withdraw(anyString(), any());
        verifyNoInteractions(transferClient);
    }

    @Test
    void processTransfer_shouldCallCreateTransfer() {
        // Arrange
        when(transferClient.createTransfer(anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        CompletableFuture<Void> result = gatewayService.processTransfer("fromUser", "toUser", 200);

        // Assert
        assertNotNull(result);
        verify(transferClient).createTransfer(anyString(), any());
        verifyNoInteractions(cashClient);
    }
}
