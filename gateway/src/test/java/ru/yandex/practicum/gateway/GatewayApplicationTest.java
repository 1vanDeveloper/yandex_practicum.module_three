package ru.yandex.practicum.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class GatewayApplicationTest {

    @Test
    void contextLoads() {
        // Проверяем, что контекст приложения загружается корректно
    }
}
