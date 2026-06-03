package ru.yandex.practicum.accounts;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

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

    @Bean(name = "AccountsThreadPoolTaskExecutor")
    public Executor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(24);
        executor.setThreadNamePrefix("AccountsPool-");
        executor.initialize();
        return executor;
    }
}
