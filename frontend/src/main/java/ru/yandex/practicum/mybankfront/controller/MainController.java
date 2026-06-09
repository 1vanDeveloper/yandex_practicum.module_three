package ru.yandex.practicum.mybankfront.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.mybankfront.controller.dto.CashAction;
import ru.yandex.practicum.mybankfront.dto.AccountResponse;
import ru.yandex.practicum.mybankfront.service.GatewayService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Контроллер main.html.
 *
 * Используемая модель для main.html:
 *      model.addAttribute("name", name);
 *      model.addAttribute("birthdate", birthdate.format(DateTimeFormatter.ISO_DATE));
 *      model.addAttribute("sum", sum);
 *      model.addAttribute("accounts", accounts);
 *      model.addAttribute("errors", errors);
 *      model.addAttribute("info", info);
 *
 * Поля модели:
 *      name - Фамилия Имя текущего пользователя, String (обязательное)
 *      birthdate - дата рождения текущего пользователя, String в формате 'YYYY-MM-DD' (обязательное)
 *      sum - сумма на счету текущего пользователя, Integer (обязательное)
 *      accounts - список аккаунтов, которым можно перевести деньги, List<AccountDto> (обязательное)
 *      errors - список ошибок после выполнения действий, List<String> (не обязательное)
 *      info - строка успешности после выполнения действия, String (не обязательное)
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class MainController {

    private final GatewayService gatewayService;

    /**
     * GET /.
     * Редирект на GET /account
     */
    @GetMapping
    public String index() {
        return "redirect:/account";
    }

    /**
     * GET /account.
     * Что нужно сделать:
     * 1. Сходить в сервис accounts через Gateway API для получения данных аккаунта по REST
     * 2. Заполнить модель main.html полученными из ответа данными
     * 3. Текущего пользователя можно получить из контекста Security
     */
    @GetMapping("/account")
    public CompletableFuture<String> getAccount(Model model, @AuthenticationPrincipal Jwt jwt) {
        String login = jwt.getClaimAsString("preferred_username");
        log.info("GET /account received for login: {}", login);

        return gatewayService.getAccount(login)
                .thenApply(account -> {
                    fillModel(model, account, null, null);
                    return "main";
                })
                .exceptionally(ex -> {
                    log.error("Error getting account for login: {}", login, ex);
                    List<String> errors = new ArrayList<>();
                    errors.add("Ошибка получения данных аккаунта: " + ex.getCause().getMessage());
                    fillModel(model, null, errors, null);
                    return "main";
                });
    }

    /**
     * POST /account.
     * Что нужно сделать:
     * 1. Сходить в сервис accounts через Gateway API для изменения данных текущего пользователя по REST
     * 2. Заполнить модель main.html полученными из ответа данными
     * 3. Текущего пользователя можно получить из контекста Security
     *
     * Изменяемые данные:
     * 1. name - Фамилия Имя
     * 2. birthdate - дата рождения в формате YYYY-DD-MM
     */
    @PostMapping("/account")
    public CompletableFuture<String> editAccount(
            Model model,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("name") String name,
            @RequestParam("birthdate") LocalDate birthdate) {

        String login = jwt.getClaimAsString("preferred_username");
        log.info("POST /account received for login: {}, name: {}, birthdate: {}", login, name, birthdate);

        // Parse name into firstName and lastName
        String[] nameParts = name.split(" ", 2);
        String firstName = nameParts.length > 0 ? nameParts[0] : "";
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        return gatewayService.updateAccount(login, firstName, lastName, birthdate.format(DateTimeFormatter.ISO_DATE))
                .thenCompose(updatedAccount -> 
                    gatewayService.getAccount(login)
                        .thenApply(account -> {
                            fillModel(model, account, null, "Данные успешно обновлены");
                            return "main";
                        })
                )
                .exceptionally(ex -> {
                    log.error("Error updating account for login: {}", login, ex);
                    List<String> errors = new ArrayList<>();
                    errors.add("Ошибка обновления данных: " + ex.getCause().getMessage());
                    fillModel(model, null, errors, null);
                    return "main";
                });
    }

    /**
     * POST /cash.
     * Что нужно сделать:
     * 1. Сходить в сервис cash через Gateway API для снятия/пополнения счета текущего аккаунта по REST
     * 2. Заполнить модель main.html полученными из ответа данными
     * 3. Текущего пользователя можно получить из контекста Security
     *
     * Параметры:
     * 1. value - сумма списания
     * 2. action - GET (снять), PUT (пополнить)
     */
    @PostMapping("/cash")
    public CompletableFuture<String> editCash(
            Model model,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("value") int value,
            @RequestParam("action") CashAction action) {

        String login = jwt.getClaimAsString("preferred_username");
        log.info("POST /cash received for login: {}, action: {}, value: {}", login, action, value);

        return gatewayService.processCash(login, value, action.name())
                .thenCompose(v -> gatewayService.getAccount(login))
                .thenApply(account -> {
                    String info = action == CashAction.PUT
                            ? "Счёт успешно пополнен на " + value
                            : "Со счёта успешно снято " + value;
                    fillModel(model, account, null, info);
                    return "main";
                })
                .exceptionally(ex -> {
                    log.error("Error processing cash for login: {}", login, ex);
                    List<String> errors = new ArrayList<>();
                    errors.add("Ошибка операции со счётом: " + ex.getCause().getMessage());
                    fillModel(model, null, errors, null);
                    return "main";
                });
    }

    /**
     * POST /transfer.
     * Что нужно сделать:
     * 1. Сходить в сервис accounts через Gateway API для перевода со счета текущего аккаунта на счет другого аккаунта по REST
     * 2. Заполнить модель main.html полученными из ответа данными
     * 3. Текущего пользователя можно получить из контекста Security
     *
     * Параметры:
     * 1. value - сумма списания
     * 2. login - логин пользователя получателя
     */
    @PostMapping("/transfer")
    public CompletableFuture<String> transfer(
            Model model,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("value") int value,
            @RequestParam("login") String toLogin) {

        String fromLogin = jwt.getClaimAsString("preferred_username");
        log.info("POST /transfer received from: {} to: {}, value: {}", fromLogin, toLogin, value);

        return gatewayService.processTransfer(fromLogin, value, toLogin)
                .thenCompose(v -> gatewayService.getAccount(fromLogin))
                .thenApply(account -> {
                    fillModel(model, account, null, "Перевод успешно выполнен");
                    return "main";
                })
                .exceptionally(ex -> {
                    log.error("Error processing transfer from: {} to: {}", fromLogin, toLogin, ex);
                    List<String> errors = new ArrayList<>();
                    errors.add("Ошибка перевода: " + ex.getCause().getMessage());
                    fillModel(model, null, errors, null);
                    return "main";
                });
    }

    /**
     * Заполняет модель данными аккаунта.
     */
    private void fillModel(Model model, AccountResponse account, List<String> errors, String info) {
        if (account != null) {
            String fullName = (account.getFirstName() != null ? account.getFirstName() : "") +
                    " " + (account.getLastName() != null ? account.getLastName() : "");
            model.addAttribute("name", fullName.trim());
            model.addAttribute("birthdate", account.getBirthDate() != null
                    ? account.getBirthDate().format(DateTimeFormatter.ISO_DATE)
                    : "");
            model.addAttribute("sum", account.getAmount() != null
                    ? account.getAmount().intValue()
                    : 0);
        } else {
            model.addAttribute("name", "");
            model.addAttribute("birthdate", "");
            model.addAttribute("sum", 0);
        }

        // Empty list of accounts for transfer (can be populated from another service)
        model.addAttribute("accounts", new ArrayList<>());

        if (errors != null && !errors.isEmpty()) {
            model.addAttribute("errors", errors);
        }
        if (info != null && !info.isEmpty()) {
            model.addAttribute("info", info);
        }
    }
}
