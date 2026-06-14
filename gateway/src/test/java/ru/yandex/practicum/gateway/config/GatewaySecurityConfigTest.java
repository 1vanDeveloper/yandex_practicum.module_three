package ru.yandex.practicum.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit-тесты для GatewaySecurityConfig
 */
@SpringBootTest
@ActiveProfiles("test")
class GatewaySecurityConfigTest {

    @Autowired
    private SecurityWebFilterChain securityWebFilterChain;

    @Test
    void securityWebFilterChainShouldBeCreated() {
        assertNotNull(securityWebFilterChain, "SecurityWebFilterChain должен быть создан");
    }
}
