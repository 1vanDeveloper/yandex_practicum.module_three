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
import ru.yandex.practicum.accounts.entity.Account;
import ru.yandex.practicum.accounts.repository.AccountRepository;
import ru.yandex.practicum.accounts.service.TestOutboxConfig;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
}
