package ru.yandex.practicum.accounts.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.accounts.dto.AccountResponse;
import ru.yandex.practicum.accounts.dto.CreateAccountRequest;
import ru.yandex.practicum.accounts.dto.UpdateAccountRequest;
import ru.yandex.practicum.accounts.entity.Account;

@Component
public class AccountMapper {

    public Account toEntity(CreateAccountRequest request) {
        return Account.builder()
                .login(request.getLogin())
                .password(request.getPassword())
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .birthDate(request.getBirthDate())
                .amount(request.getAmount())
                .build();
    }

    public AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .login(account.getLogin())
                .firstName(account.getFirstName())
                .lastName(account.getLastName())
                .email(account.getEmail())
                .birthDate(account.getBirthDate())
                .amount(account.getAmount())
                .build();
    }

    public void updateEntityFromRequest(UpdateAccountRequest request, Account account) {
        if (request.getFirstName() != null) {
            account.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            account.setLastName(request.getLastName());
        }
        if (request.getEmail() != null) {
            account.setEmail(request.getEmail());
        }
        if (request.getBirthDate() != null) {
            account.setBirthDate(request.getBirthDate());
        }
        if (request.getAmount() != null) {
            account.setAmount(request.getAmount());
        }
    }
}
