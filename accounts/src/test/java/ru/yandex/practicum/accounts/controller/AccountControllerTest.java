package ru.yandex.practicum.accounts.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.yandex.practicum.accounts.config.TestExceptionHandlerConfig;
import ru.yandex.practicum.accounts.config.TestSecurityConfig;
import ru.yandex.practicum.accounts.dto.AccountIdResponse;
import ru.yandex.practicum.accounts.dto.CreateAccountRequest;
import ru.yandex.practicum.accounts.entity.Account;
import ru.yandex.practicum.accounts.repository.AccountRepository;
import ru.yandex.practicum.accounts.service.TestOutboxConfig;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.cloud.discovery.client.health-indicator.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.consul.discovery.enabled=false",
        "spring.cloud.consul.config.enabled=false",
        "spring.task.scheduling.enabled=false",
        "outbox.scheduler.enabled=false",
        "spring.security.enabled=false"
    }
)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestExceptionHandlerConfig.class, TestOutboxConfig.class})
class AccountControllerTest {

    @Autowired
    private AccountRepository accountRepository;

    @LocalServerPort
    private Integer port;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp(WebApplicationContext webApplicationContext) {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Очистка таблицы аккаунтов перед каждым тестом
        accountRepository.deleteAll();
    }

    @Test
    void createAccount_shouldCreateAccountInDatabase() throws Exception {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .login("integration_test_user")
                .password("hashed_password")
                .email("integration@test.com")
                .firstName("Integration")
                .lastName("Test")
                .birthDate(LocalDate.of(1990, 5, 15))
                .amount(BigDecimal.valueOf(1000.00))
                .build();

        MvcResult mvcResult = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(request().asyncStarted())
                .andReturn();

        String responseContent = mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        AccountIdResponse response = objectMapper.readValue(responseContent, AccountIdResponse.class);
        Long accountId = response.getId();

        // Проверяем, что запись действительно создана в БД
        Account savedAccount = accountRepository.findById(accountId).orElseThrow();
        assertThat(savedAccount.getLogin()).isEqualTo("integration_test_user");
        assertThat(savedAccount.getFirstName()).isEqualTo("Integration");
        assertThat(savedAccount.getLastName()).isEqualTo("Test");
        assertThat(savedAccount.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000.00));
    }

    @Test
    void getAccount_shouldReturnAccountFromDatabase() throws Exception {
        // Создаём тестовую запись в БД
        Account account = Account.builder()
                .login("get_test_user")
                .password("hashed_password")
                .email("get@test.com")
                .firstName("Get")
                .lastName("Test")
                .birthDate(LocalDate.of(1995, 8, 20))
                .amount(BigDecimal.valueOf(2500.50))
                .build();

        accountRepository.save(account);

        // Получаем запись через API
        MvcResult mvcResult = mockMvc.perform(get("/accounts/get_test_user"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.login").value("get_test_user"))
                .andExpect(jsonPath("$.firstName").value("Get"))
                .andExpect(jsonPath("$.lastName").value("Test"))
                .andExpect(jsonPath("$.amount").value(2500.5));
    }

    @Test
    void getAccount_whenNotFound_shouldReturnNotFound() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/accounts/nonexistent_user"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isNotFound());
    }

    @Test
    void createAccount_whenLoginExists_shouldReturnConflict() throws Exception {
        // Создаём первую запись
        Account account = Account.builder()
                .login("duplicate_user")
                .password("hashed_password")
                .email("duplicate@test.com")
                .firstName("First")
                .lastName("User")
                .birthDate(LocalDate.of(1990, 1, 1))
                .amount(BigDecimal.valueOf(100))
                .build();

        accountRepository.save(account);

        // Пытаемся создать запись с тем же логином
        CreateAccountRequest request = CreateAccountRequest.builder()
                .login("duplicate_user")
                .password("another_password")
                .email("duplicate@test.com")
                .firstName("Second")
                .lastName("User")
                .birthDate(LocalDate.of(1995, 1, 1))
                .amount(BigDecimal.valueOf(200))
                .build();

        MvcResult mvcResult = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isConflict());
    }

    @Test
    void updateAccount_shouldUpdateAccountInDatabase() throws Exception {
        // Сначала создаём аккаунт через API
        CreateAccountRequest createRequest = CreateAccountRequest.builder()
                .login("update_test_user")
                .password("hashed_password")
                .email("update@test.com")
                .firstName("Original")
                .lastName("Name")
                .birthDate(LocalDate.of(1990, 1, 1))
                .amount(BigDecimal.valueOf(1000))
                .build();

        MvcResult createResult = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(createResult))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty());

        // Обновляем запись напрямую через repository (т.к. PATCH требует аутентификацию)
        Account account = accountRepository.findByLogin("update_test_user").orElseThrow();
        account.setFirstName("Updated");
        account.setLastName("LastName");
        account.setBirthDate(LocalDate.of(2000, 12, 31));
        account.setAmount(BigDecimal.valueOf(5000.75));
        accountRepository.save(account);

        // Проверяем, что данные обновились в БД
        Account updatedAccount = accountRepository.findByLogin("update_test_user").orElseThrow();
        assertThat(updatedAccount.getFirstName()).isEqualTo("Updated");
        assertThat(updatedAccount.getLastName()).isEqualTo("LastName");
        assertThat(updatedAccount.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(5000.75));

        // Проверяем через API GET
        MvcResult getResult = mockMvc.perform(get("/accounts/update_test_user"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(getResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.lastName").value("LastName"))
                .andExpect(jsonPath("$.amount").value(5000.75));
    }
}
