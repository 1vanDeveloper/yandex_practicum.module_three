package ru.yandex.practicum.accounts.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.accounts.dto.AccountResponse;
import ru.yandex.practicum.accounts.dto.CreateAccountRequest;
import ru.yandex.practicum.accounts.dto.UpdateAccountRequest;
import ru.yandex.practicum.accounts.entity.Account;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AccountMapperTest {

    private AccountMapper accountMapper;

    @BeforeEach
    void setUp() {
        accountMapper = new AccountMapper();
    }

    @Test
    void toEntity_shouldMapCreateRequestToEntity() {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .login("test_user")
                .password("hashed_password")
                .firstName("Test")
                .lastName("User")
                .birthDate(LocalDate.of(1990, 5, 15))
                .amount(BigDecimal.valueOf(1000.00))
                .build();

        Account result = accountMapper.toEntity(request);

        assertThat(result.getLogin()).isEqualTo("test_user");
        assertThat(result.getPassword()).isEqualTo("hashed_password");
        assertThat(result.getFirstName()).isEqualTo("Test");
        assertThat(result.getLastName()).isEqualTo("User");
        assertThat(result.getBirthDate()).isEqualTo(LocalDate.of(1990, 5, 15));
        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000.00));
    }

    @Test
    void toResponse_shouldMapEntityToResponse() {
        Account account = Account.builder()
                .id(1L)
                .login("test_user")
                .firstName("Test")
                .lastName("User")
                .birthDate(LocalDate.of(1990, 5, 15))
                .amount(BigDecimal.valueOf(1000.00))
                .build();

        AccountResponse result = accountMapper.toResponse(account);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getLogin()).isEqualTo("test_user");
        assertThat(result.getFirstName()).isEqualTo("Test");
        assertThat(result.getLastName()).isEqualTo("User");
        assertThat(result.getBirthDate()).isEqualTo(LocalDate.of(1990, 5, 15));
        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000.00));
    }

    @Test
    void updateEntityFromRequest_shouldUpdateOnlyProvidedFields() {
        Account account = Account.builder()
                .id(1L)
                .login("test_user")
                .firstName("Test")
                .lastName("User")
                .birthDate(LocalDate.of(1990, 5, 15))
                .amount(BigDecimal.valueOf(1000.00))
                .build();

        UpdateAccountRequest request = UpdateAccountRequest.builder()
                .firstName("Updated")
                .lastName(null)
                .birthDate(null)
                .amount(BigDecimal.valueOf(2000.00))
                .build();

        accountMapper.updateEntityFromRequest(request, account);

        assertThat(account.getFirstName()).isEqualTo("Updated");
        assertThat(account.getLastName()).isEqualTo("User");
        assertThat(account.getBirthDate()).isEqualTo(LocalDate.of(1990, 5, 15));
        assertThat(account.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(2000.00));
    }
}
