package ru.yandex.practicum.accounts.config;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.accounts.service.AccountService;

@RestControllerAdvice
public class TestExceptionHandlerConfig {

    @ExceptionHandler(AccountService.AccountNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    void handleNotFound(AccountService.AccountNotFoundException ex) {
    }

    @ExceptionHandler(AccountService.AccountAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    void handleAlreadyExists(AccountService.AccountAlreadyExistsException ex) {
    }
}
