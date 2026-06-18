package ru.yandex.practicum.frontend.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ConcurrentModel;
import ru.yandex.practicum.frontend.controller.dto.CashAction;
import ru.yandex.practicum.frontend.dto.AccountBrief;
import ru.yandex.practicum.frontend.dto.AccountResponse;
import ru.yandex.practicum.frontend.service.GatewayService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Интеграционные тесты контроллеров frontend сервиса.
 * Тестируют методы контроллеров напрямую с мокированным GatewayService.
 */
class MainControllerIntegrationTest {

    private MainController mainController;
    private AuthController authController;
    private GatewayService gatewayService;

    @BeforeEach
    void setUp() {
        gatewayService = mock(GatewayService.class);
        mainController = new MainController(gatewayService);
        authController = new AuthController(gatewayService);
    }

    @Test
    @DisplayName("Страница входа - GET /login")
    void loginPage() {
        String view = authController.loginPage();
        assertEquals("login", view);
    }

    @Test
    @DisplayName("Страница регистрации - GET /register")
    void registerPage() {
        String view = authController.registerPage();
        assertEquals("register", view);
    }

    @Test
    @DisplayName("Регистрация пользователя через frontend - успешная регистрация")
    void register_success() {
        when(gatewayService.register(any())).thenReturn(CompletableFuture.completedFuture(null));

        String view = authController.register(
                "testuser",
                "password123",
                "test@example.com",
                "Test",
                "User",
                "1990-01-01"
        ).getUrl();

        assertEquals("/login?registered", view);
        verify(gatewayService).register(any());
    }

    @Test
    @DisplayName("Регистрация пользователя через frontend - ошибка валидации")
    void register_validationError() {
        when(gatewayService.register(any())).thenThrow(new IllegalArgumentException("Login is required"));

        String view = authController.register(
                "",
                "password123",
                "test@example.com",
                "Test",
                "User",
                "1990-01-01"
        ).getUrl();

        assertEquals("/register?error", view);
    }

    @Test
    @DisplayName("Главная страница без авторизации - редирект на /login")
    void index_withoutAuth() {
        String redirect = authController.index();
        assertEquals("redirect:/login", redirect);
    }

    @Test
    @DisplayName("Получение данных аккаунта через frontend - GET /account")
    void getAccount_throughController() throws Exception {
        AccountResponse testAccount = AccountResponse.builder()
                .id(1L)
                .login("testuser")
                .firstName("Test")
                .lastName("User")
                .birthDate(LocalDate.of(1990, 1, 1))
                .amount(BigDecimal.valueOf(1000.00))
                .build();

        List<AccountBrief> accounts = new ArrayList<>();
        accounts.add(new AccountBrief("other_user", "Other User"));

        when(gatewayService.getAccount(anyString())).thenReturn(CompletableFuture.completedFuture(testAccount));
        when(gatewayService.getAccountBriefs(anyString())).thenReturn(CompletableFuture.completedFuture(accounts));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("JWT_TOKEN", "test-token");

        String view = mainController.getAccount(new ConcurrentModel(), request).join();

        assertEquals("main", view);
        verify(gatewayService).getAccount(anyString());
        verify(gatewayService).getAccountBriefs(anyString());
    }

    @Test
    @DisplayName("Пополнение счёта через frontend - POST /cash PUT")
    void depositCash_throughController() throws Exception {
        AccountResponse testAccount = AccountResponse.builder()
                .id(1L)
                .login("testuser")
                .firstName("Test")
                .lastName("User")
                .birthDate(LocalDate.of(1990, 1, 1))
                .amount(BigDecimal.valueOf(1100.00))
                .build();

        List<AccountBrief> accounts = new ArrayList<>();

        when(gatewayService.processCash(eq(100), eq("PUT"), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(gatewayService.getAccount(anyString())).thenReturn(CompletableFuture.completedFuture(testAccount));
        when(gatewayService.getAccountBriefs(anyString())).thenReturn(CompletableFuture.completedFuture(accounts));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("JWT_TOKEN", "test-token");

        String view = mainController.editCash(new ConcurrentModel(), 100, CashAction.PUT, request).join();

        assertEquals("main", view);
        verify(gatewayService).processCash(eq(100), eq("PUT"), anyString());
    }

    @Test
    @DisplayName("Снятие средств через frontend - POST /cash GET")
    void withdrawCash_throughController() throws Exception {
        AccountResponse testAccount = AccountResponse.builder()
                .id(1L)
                .login("testuser")
                .firstName("Test")
                .lastName("User")
                .birthDate(LocalDate.of(1990, 1, 1))
                .amount(BigDecimal.valueOf(900.00))
                .build();

        List<AccountBrief> accounts = new ArrayList<>();

        when(gatewayService.processCash(eq(100), eq("GET"), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(gatewayService.getAccount(anyString())).thenReturn(CompletableFuture.completedFuture(testAccount));
        when(gatewayService.getAccountBriefs(anyString())).thenReturn(CompletableFuture.completedFuture(accounts));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("JWT_TOKEN", "test-token");

        String view = mainController.editCash(new ConcurrentModel(), 100, CashAction.GET, request).join();

        assertEquals("main", view);
        verify(gatewayService).processCash(eq(100), eq("GET"), anyString());
    }

    @Test
    @DisplayName("Перевод средств через frontend - POST /transfer")
    void transfer_throughController() throws Exception {
        AccountResponse testAccount = AccountResponse.builder()
                .id(1L)
                .login("testuser")
                .firstName("Test")
                .lastName("User")
                .birthDate(LocalDate.of(1990, 1, 1))
                .amount(BigDecimal.valueOf(500.00))
                .build();

        List<AccountBrief> accounts = new ArrayList<>();

        when(gatewayService.processTransfer(eq(500), eq("recipient"), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(gatewayService.getAccount(anyString())).thenReturn(CompletableFuture.completedFuture(testAccount));
        when(gatewayService.getAccountBriefs(anyString())).thenReturn(CompletableFuture.completedFuture(accounts));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("JWT_TOKEN", "test-token");

        String view = mainController.transfer(new ConcurrentModel(), 500, "recipient", request).join();

        assertEquals("main", view);
        verify(gatewayService).processTransfer(eq(500), eq("recipient"), anyString());
    }

    @Test
    @DisplayName("Обновление данных аккаунта через frontend - POST /account")
    void updateAccount_throughController() throws Exception {
        AccountResponse testAccount = AccountResponse.builder()
                .id(1L)
                .login("testuser")
                .firstName("Updated")
                .lastName("Name")
                .birthDate(LocalDate.of(1995, 5, 15))
                .amount(BigDecimal.valueOf(1000.00))
                .build();

        List<AccountBrief> accounts = new ArrayList<>();

        when(gatewayService.updateAccount(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(testAccount));
        when(gatewayService.getAccount(anyString())).thenReturn(CompletableFuture.completedFuture(testAccount));
        when(gatewayService.getAccountBriefs(anyString())).thenReturn(CompletableFuture.completedFuture(accounts));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("JWT_TOKEN", "test-token");

        String view = mainController.editAccount(new ConcurrentModel(), "Updated Name", LocalDate.of(1995, 5, 15), request).join();

        assertEquals("main", view);
        verify(gatewayService).updateAccount(anyString(), anyString(), anyString(), anyString());
    }
}
