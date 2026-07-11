package ru.yandex.practicum.accounts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.yandex.practicum.accounts.config.TracingConfig;

@SpringBootApplication
@EnableAsync
@Import(TracingConfig.class)
public class AccountsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountsApplication.class, args);
    }
}

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "spring.scheduling.enabled", havingValue = "true", matchIfMissing = true)
class SchedulingConfig {
}
