package ru.yandex.practicum.transfer.exception;

public class SelfTransferException extends RuntimeException {
    public SelfTransferException(String message) {
        super(message);
    }
}
