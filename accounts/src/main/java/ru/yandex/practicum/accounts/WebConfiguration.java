package ru.yandex.practicum.accounts;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
@PropertySource("classpath:application.properties")
public class WebConfiguration {

    @Bean
    public OpenAPI paymentsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Accounts API")
                        .description("REST API для управления аккаунтами")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Yandex Practicum")
                                .email("support@yandex.ru")));
    }
}
