package ru.yandex.practicum.transfer.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.transfer.dto.TransferResponse;
import ru.yandex.practicum.transfer.entity.Transfer;

@Component
public class TransferMapper {

    public TransferResponse toResponse(Transfer transfer) {
        if (transfer == null) {
            return null;
        }
        return new TransferResponse(
                transfer.getId(),
                transfer.getFromAccountLogin(),
                transfer.getToAccountLogin(),
                transfer.getAmount(),
                transfer.getStatus(),
                transfer.getErrorMessage(),
                transfer.getCreatedAt(),
                transfer.getUpdatedAt()
        );
    }
}
