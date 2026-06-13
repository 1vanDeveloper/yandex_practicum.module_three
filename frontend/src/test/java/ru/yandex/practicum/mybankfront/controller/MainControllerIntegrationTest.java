package ru.yandex.practicum.mybankfront.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.validation.BindingResult;
import ru.yandex.practicum.mybankfront.controller.dto.CashAction;
import ru.yandex.practicum.mybankfront.dto.AccountResponse;
import ru.yandex.practicum.mybankfront.dto.RegisterRequest;
import ru.yandex.practicum.mybankfront.service.GatewayService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        String view = authController.loginPage(new ConcurrentModel());
        assertEquals("login", view);
    }

    @Test
    @DisplayName("Страница регистрации - GET /register")
    void registerPage() {
        String view = authController.registerPage(new ConcurrentModel());
        assertEquals("register", view);
    }

    @Test
    @DisplayName("Регистрация пользователя через frontend - успешная регистрация")
    void register_success() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setLogin("testuser");
        request.setPassword("password123");
        request.setEmail("test@example.com");
        request.setFirstName("Test");
        request.setLastName("User");
        request.setBirthDate(LocalDate.of(1990, 1, 1));
        request.setAmount(BigDecimal.valueOf(1000));

        when(gatewayService.register(any(RegisterRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);

        String view = authController.register(request, bindingResult, new ConcurrentModel());

        assertEquals("login", view);
        verify(gatewayService).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Регистрация пользователя через frontend - ошибка валидации")
    void register_validationError() {
        RegisterRequest request = new RegisterRequest();
        request.setLogin("");

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(true);

        String view = authController.register(request, bindingResult, new ConcurrentModel());

        assertEquals("register", view);
    }

    @Test
    @DisplayName("Главная страница без авторизации - редирект на /login")
    void index_withoutAuth() {
        String redirect = authController.index(null);
        assertEquals("redirect:/login", redirect);
    }

    @Test
    @DisplayName("Главная страница с авторизацией - редирект на /account")
    void index_withAuth() {
        MockJwt jwt = new MockJwt("testuser");
        String redirect = authController.index(jwt);
        assertEquals("redirect:/account", redirect);
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

        when(gatewayService.getAccount("testuser"))
                .thenReturn(CompletableFuture.completedFuture(testAccount));

        ConcurrentModel model = new ConcurrentModel();
        MockJwt jwt = new MockJwt("testuser");

        String view = mainController.getAccount(model, jwt).join();

        assertEquals("main", view);
        assertEquals("Test User", model.getAttribute("name"));
        assertEquals(1000, model.getAttribute("sum"));
        verify(gatewayService).getAccount("testuser");
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

        when(gatewayService.processCash(eq("testuser"), eq(100), eq("PUT")))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(gatewayService.getAccount("testuser"))
                .thenReturn(CompletableFuture.completedFuture(testAccount));

        ConcurrentModel model = new ConcurrentModel();
        MockJwt jwt = new MockJwt("testuser");

        String view = mainController.editCash(model, jwt, 100, CashAction.PUT).join();

        assertEquals("main", view);
        assertEquals(1100, model.getAttribute("sum"));
        assertNotNull(model.getAttribute("info"));
        verify(gatewayService).processCash("testuser", 100, CashAction.PUT.name());
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

        when(gatewayService.processCash(eq("testuser"), eq(100), eq("GET")))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(gatewayService.getAccount("testuser"))
                .thenReturn(CompletableFuture.completedFuture(testAccount));

        ConcurrentModel model = new ConcurrentModel();
        MockJwt jwt = new MockJwt("testuser");

        String view = mainController.editCash(model, jwt, 100, CashAction.GET).join();

        assertEquals("main", view);
        assertEquals(900, model.getAttribute("sum"));
        assertNotNull(model.getAttribute("info"));
        verify(gatewayService).processCash("testuser", 100, CashAction.GET.name());
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

        when(gatewayService.processTransfer(eq("testuser"), eq(500), eq("recipient")))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(gatewayService.getAccount("testuser"))
                .thenReturn(CompletableFuture.completedFuture(testAccount));

        ConcurrentModel model = new ConcurrentModel();
        MockJwt jwt = new MockJwt("testuser");

        String view = mainController.transfer(model, jwt, 500, "recipient").join();

        assertEquals("main", view);
        assertEquals(500, model.getAttribute("sum"));
        assertNotNull(model.getAttribute("info"));
        verify(gatewayService).processTransfer("testuser", 500, "recipient");
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

        when(gatewayService.updateAccount(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(testAccount));
        when(gatewayService.getAccount("testuser"))
                .thenReturn(CompletableFuture.completedFuture(testAccount));

        ConcurrentModel model = new ConcurrentModel();
        MockJwt jwt = new MockJwt("testuser");

        String view = mainController.editAccount(model, jwt, "Updated Name", LocalDate.of(1995, 5, 15)).join();

        assertEquals("main", view);
        assertEquals("Updated Name", model.getAttribute("name"));
        assertNotNull(model.getAttribute("info"));
        verify(gatewayService).updateAccount(anyString(), anyString(), anyString(), anyString());
    }
}
