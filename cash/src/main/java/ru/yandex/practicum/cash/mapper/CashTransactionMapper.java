package ru.yandex.practicum.cash.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.cash.dto.TransactionResponse;
import ru.yandex.practicum.cash.entity.CashTransaction;

@Component
public class CashTransactionMapper {

    public TransactionResponse toResponse(CashTransaction transaction) {
        if (transaction == null) {
            return null;
        }
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccountLogin(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getStatus(),
                transaction.getErrorMessage(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }
}
