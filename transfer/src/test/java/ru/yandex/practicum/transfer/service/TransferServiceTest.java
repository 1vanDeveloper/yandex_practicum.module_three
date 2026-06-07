package ru.yandex.practicum.transfer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import ru.yandex.practicum.transfer.client.AccountsClient;
import ru.yandex.practicum.transfer.client.NotificationsClient;
import ru.yandex.practicum.transfer.dto.TransferRequest;
import ru.yandex.practicum.transfer.dto.TransferResponse;
import ru.yandex.practicum.transfer.entity.Transfer;
import ru.yandex.practicum.transfer.entity.TransferStatus;
import ru.yandex.practicum.transfer.exception.InsufficientFundsException;
import ru.yandex.practicum.transfer.exception.SelfTransferException;
import ru.yandex.practicum.transfer.mapper.TransferMapper;
import ru.yandex.practicum.transfer.repository.TransferRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
    private NotificationsClient notificationsClient;

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

        // When
        CompletableFuture<TransferResponse> future = transferService.createTransfer(request);

        // Then
        assertTrue(future.isCompletedExceptionally());
        try {
            future.join();
            fail("Expected exception to be thrown");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof SelfTransferException);
        }
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
        when(notificationsClient.sendNotification(any(), eq("test-token")))
                .thenReturn(CompletableFuture.completedFuture(null));

        // When
        CompletableFuture<TransferResponse> future = transferService.createTransfer(request);
        TransferResponse response = future.join();

        // Then
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(TransferStatus.COMPLETED, response.status());
        verify(accountsClient).debitAccount(eq("sender"), eq(new BigDecimal("100.00")), eq("test-token"));
        verify(transferRepository).save(any(Transfer.class));
    }

    @Test
    void testCreateTransfer_whenInsufficientFunds_throwsException() {
        // Given
        TransferRequest request = new TransferRequest("sender", "receiver", new BigDecimal("1000.00"), null);

        when(accountsClient.debitAccount(eq("sender"), eq(new BigDecimal("1000.00")), eq("test-token")))
                .thenReturn(CompletableFuture.failedFuture(new InsufficientFundsException("Insufficient funds")));

        // When
        CompletableFuture<TransferResponse> future = transferService.createTransfer(request);

        // Then
        assertTrue(future.isCompletedExceptionally());
        try {
            future.join();
            fail("Expected exception to be thrown");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof InsufficientFundsException);
        }
    }

    @Test
    void testGetTransfersByLogin_whenTransfersExist_returnsList() {
        // Given
        String login = "test_user";
        List<Transfer> transfers = List.of(
                createTransfer(1L, login, "other_user", new BigDecimal("100.00"), TransferStatus.COMPLETED),
                createTransfer(2L, "other_user", login, new BigDecimal("50.00"), TransferStatus.COMPLETED)
        );

        when(transferRepository.findByFromAccountLoginOrToAccountLoginOrderByCreatedAtDesc(login, login))
                .thenReturn(transfers);
        when(mapper.toResponse(any(Transfer.class))).thenAnswer(invocation -> {
            Transfer tx = invocation.getArgument(0);
            return new TransferResponse(
                    tx.getId(), tx.getFromAccountLogin(), tx.getToAccountLogin(),
                    tx.getAmount(), tx.getStatus(), tx.getErrorMessage(), null, null
            );
        });

        // When
        CompletableFuture<List<TransferResponse>> future = transferService.getTransfersByLogin(login);
        List<TransferResponse> result = future.join();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(transferRepository).findByFromAccountLoginOrToAccountLoginOrderByCreatedAtDesc(login, login);
    }

    @Test
    void testGetTransferById_whenExists_returnsResponse() {
        // Given
        Long id = 1L;
        Transfer transfer = createTransfer(id, "sender", "receiver", new BigDecimal("100.00"), TransferStatus.COMPLETED);
        TransferResponse expectedResponse = new TransferResponse(
                id, "sender", "receiver", new BigDecimal("100.00"),
                TransferStatus.COMPLETED, null, null, null
        );

        when(transferRepository.findById(id)).thenReturn(Optional.of(transfer));
        when(mapper.toResponse(transfer)).thenReturn(expectedResponse);

        // When
        CompletableFuture<TransferResponse> future = transferService.getTransferById(id);
        TransferResponse response = future.join();

        // Then
        assertNotNull(response);
        assertEquals(id, response.id());
        verify(transferRepository).findById(id);
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
