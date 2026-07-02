package ru.yandex.practicum.transfer.service;

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
import ru.yandex.practicum.transfer.client.AccountsClient;
import ru.yandex.practicum.transfer.dto.TransferRequest;
import ru.yandex.practicum.transfer.dto.TransferResponse;
import ru.yandex.practicum.transfer.entity.Transfer;
import ru.yandex.practicum.transfer.entity.TransferStatus;
import ru.yandex.practicum.transfer.event.TransferNotificationEvent;
import ru.yandex.practicum.transfer.exception.InsufficientFundsException;
import ru.yandex.practicum.transfer.exception.SelfTransferException;
import ru.yandex.practicum.transfer.mapper.TransferMapper;
import ru.yandex.practicum.transfer.repository.TransferRepository;
import ru.yandex.practicum.transfer.service.KafkaNotificationSender;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransferServiceTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private TransferMapper mapper;

    @Mock
    private AccountsClient accountsClient;

    @Mock
    private KafkaNotificationSender kafkaNotificationSender;

    @Mock
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @Mock
    private OAuth2AuthorizedClient authorizedClient;

    @Mock
    private OAuth2AccessToken accessToken;

    @InjectMocks
    private TransferService transferService;

    @BeforeEach
    void setUp() {
        when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class)))
                .thenReturn(authorizedClient);
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(accessToken.getTokenValue()).thenReturn("test-token");
    }

    @Test
    void testCreateTransfer_whenSelfTransfer_throwsException() {
        // Given
        TransferRequest request = new TransferRequest("same_user", "same_user", new BigDecimal("100.00"), null);

        // When & Then
        assertThrows(SelfTransferException.class, () -> transferService.createTransfer(request));
    }

    @Test
    void testCreateTransfer_whenSuccessful_returnsTransferResponse() {
        // Given
        TransferRequest request = new TransferRequest("sender", "receiver", new BigDecimal("100.00"), null);
        Transfer savedTransfer = createTransfer(1L, "sender", "receiver", new BigDecimal("100.00"), TransferStatus.COMPLETED);
        TransferResponse expectedResponse = new TransferResponse(
                1L, "sender", "receiver", new BigDecimal("100.00"),
                TransferStatus.COMPLETED, null, null, null
        );

        when(accountsClient.debitAccount(eq("sender"), eq(new BigDecimal("100.00")), eq("test-token")))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(accountsClient.creditAccount(eq("receiver"), eq(new BigDecimal("100.00")), eq("test-token")))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(transferRepository.save(any(Transfer.class))).thenReturn(savedTransfer);
        when(mapper.toResponse(savedTransfer)).thenReturn(expectedResponse);
        when(kafkaNotificationSender.sendNotification(any(TransferNotificationEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        // When
        TransferResponse response = transferService.createTransfer(request);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(TransferStatus.COMPLETED, response.status());
        verify(accountsClient).debitAccount(eq("sender"), eq(new BigDecimal("100.00")), eq("test-token"));
        verify(transferRepository).save(any(Transfer.class));
        verify(kafkaNotificationSender, times(2)).sendNotification(any(TransferNotificationEvent.class));
    }

    @Test
    void testCreateTransfer_whenInsufficientFunds_throwsException() {
        // Given
        TransferRequest request = new TransferRequest("sender", "receiver", new BigDecimal("1000.00"), null);

        when(accountsClient.debitAccount(eq("sender"), eq(new BigDecimal("1000.00")), eq("test-token")))
                .thenReturn(CompletableFuture.failedFuture(new InsufficientFundsException("Insufficient funds")));

        // When & Then
        CompletionException exception = assertThrows(CompletionException.class, () -> transferService.createTransfer(request));
        assertTrue(exception.getCause() instanceof InsufficientFundsException);
    }

    private Transfer createTransfer(Long id, String from, String to, BigDecimal amount, TransferStatus status) {
        Transfer tx = new Transfer();
        tx.setId(id);
        tx.setFromAccountLogin(from);
        tx.setToAccountLogin(to);
        tx.setAmount(amount);
        tx.setStatus(status);
        return tx;
    }
}
