package ru.yandex.practicum.accounts.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.accounts.service.AccountService;

@RestControllerAdvice
@Configuration
public class TestExceptionHandlerConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @ExceptionHandler(AccountService.AccountNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    void handleNotFound(AccountService.AccountNotFoundException ex) {
    }

    @ExceptionHandler(AccountService.AccountAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    void handleAlreadyExists(AccountService.AccountAlreadyExistsException ex) {
    }
}
