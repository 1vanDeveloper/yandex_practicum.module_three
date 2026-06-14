package ru.yandex.practicum.cash.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import ru.yandex.practicum.cash.client.AccountsClient;
import ru.yandex.practicum.cash.client.NotificationsClient;
import ru.yandex.practicum.cash.dto.DepositRequest;
import ru.yandex.practicum.cash.dto.TransactionResponse;
import ru.yandex.practicum.cash.dto.WithdrawRequest;
import ru.yandex.practicum.cash.entity.CashTransaction;
import ru.yandex.practicum.cash.entity.TransactionStatus;
import ru.yandex.practicum.cash.entity.TransactionType;
import ru.yandex.practicum.cash.exception.InsufficientFundsException;
import ru.yandex.practicum.cash.mapper.CashTransactionMapper;
import ru.yandex.practicum.cash.repository.CashTransactionRepository;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CashServiceTest {

    @Mock
    private CashTransactionRepository transactionRepository;

    @Mock
    private CashTransactionMapper mapper;

    @Mock
    private AccountsClient accountsClient;

    @Mock
    private NotificationsClient notificationsClient;

    @Mock
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @Mock
    private OAuth2AuthorizedClient authorizedClient;

    @Mock
    private OAuth2AccessToken accessToken;

    @InjectMocks
    private CashService cashService;

    @BeforeEach
    void setUp() {
        when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class)))
                .thenReturn(authorizedClient);
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(accessToken.getTokenValue()).thenReturn("test-token");
    }

    @Test
    void testDeposit_whenSuccessful_returnsTransactionResponse() {
        // Given
        DepositRequest request = new DepositRequest("test_user", new BigDecimal("100.00"));
        CashTransaction savedTransaction = createTransaction(1L, "test_user", TransactionType.DEPOSIT, 
                new BigDecimal("100.00"), TransactionStatus.COMPLETED);
        TransactionResponse expectedResponse = new TransactionResponse(
                1L, "test_user", TransactionType.DEPOSIT, new BigDecimal("100.00"),
                TransactionStatus.COMPLETED, null, null, null
        );

        doNothing().when(accountsClient).deposit(eq(request), eq("test-token"));
        when(transactionRepository.save(any(CashTransaction.class))).thenReturn(savedTransaction);
        when(mapper.toResponse(savedTransaction)).thenReturn(expectedResponse);

        // When
        TransactionResponse response = cashService.deposit(request);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(TransactionStatus.COMPLETED, response.status());
        verify(accountsClient).deposit(eq(request), eq("test-token"));
        verify(transactionRepository).save(any(CashTransaction.class));
    }

    @Test
    void testWithdraw_whenSuccessful_returnsTransactionResponse() {
        // Given
        WithdrawRequest request = new WithdrawRequest("test_user", new BigDecimal("50.00"));
        CashTransaction savedTransaction = createTransaction(2L, "test_user", TransactionType.WITHDRAW,
                new BigDecimal("50.00"), TransactionStatus.COMPLETED);
        TransactionResponse expectedResponse = new TransactionResponse(
                2L, "test_user", TransactionType.WITHDRAW, new BigDecimal("50.00"),
                TransactionStatus.COMPLETED, null, null, null
        );

        doNothing().when(accountsClient).withdraw(eq(request), eq("test-token"));
        when(transactionRepository.save(any(CashTransaction.class))).thenReturn(savedTransaction);
        when(mapper.toResponse(savedTransaction)).thenReturn(expectedResponse);

        // When
        TransactionResponse response = cashService.withdraw(request);

        // Then
        assertNotNull(response);
        assertEquals(2L, response.id());
        assertEquals(TransactionStatus.COMPLETED, response.status());
        verify(accountsClient).withdraw(eq(request), eq("test-token"));
        verify(transactionRepository).save(any(CashTransaction.class));
    }

    @Test
    void testWithdraw_whenInsufficientFunds_throwsException() {
        // Given
        WithdrawRequest request = new WithdrawRequest("test_user", new BigDecimal("1000.00"));
        
        doThrow(new InsufficientFundsException("Insufficient funds"))
                .when(accountsClient).withdraw(eq(request), eq("test-token"));

        // When & Then
        assertThrows(InsufficientFundsException.class, () -> cashService.withdraw(request));
    }

    private CashTransaction createTransaction(Long id, String login, TransactionType type,
                                               BigDecimal amount, TransactionStatus status) {
        CashTransaction tx = new CashTransaction();
        tx.setId(id);
        tx.setAccountLogin(login);
        tx.setTransactionType(type);
        tx.setAmount(amount);
        tx.setStatus(status);
        return tx;
    }
}
