package ru.yandex.practicum.notifications;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:application.properties")
public class WebConfiguration {

    @Bean
    public OpenAPI notificationsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Notifications API")
                        .description("REST API для управления уведомлениями")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Yandex Practicum")
                                .email("support@yandex.ru")));
    }
}
