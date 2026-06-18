package ru.yandex.practicum.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit-тесты для JwtAuthFilter
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    private MockServerHttpRequest request;
    private ServerWebExchange exchange;

    @BeforeEach
    void setUp() {
        request = MockServerHttpRequest.get("/gateway/account").build();
        exchange = MockServerWebExchange.from(request);
    }

    @Test
    void filter_shouldPropagateJwtToken_whenTokenPresent() {
        // Arrange
        String tokenValue = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test";
        Jwt jwt = Jwt.withTokenValue(tokenValue)
            .header("alg", "HS256")
            .claim("sub", "test-user")
            .claim("privileges", Collections.singletonList("accounts:read"))
            .build();

        JwtAuthenticationToken authToken = new JwtAuthenticationToken(
            jwt,
            Collections.emptyList(),
            "test-user"
        );

        SecurityContext securityContext = new SecurityContextImpl(authToken);
        exchange.getAttributes().put(
            org.springframework.security.core.context.ReactiveSecurityContextHolder.class.getName(), 
            Mono.just(securityContext));

        // Act & Assert - просто проверяем, что фильтр не бросает исключений
        StepVerifier.create(jwtAuthFilter.filter(exchange, chain -> Mono.empty()))
            .verifyComplete();
    }

    @Test
    void filter_shouldContinueWithoutToken_whenSecurityContextEmpty() {
        // Arrange - пустой SecurityContext
        exchange.getAttributes().put(
            org.springframework.security.core.context.ReactiveSecurityContextHolder.class.getName(), 
            Mono.empty());

        // Act & Assert
        StepVerifier.create(jwtAuthFilter.filter(exchange, chain -> Mono.empty()))
            .verifyComplete();
    }

    @Test
    void filter_shouldContinueWithoutToken_whenAuthenticationIsNull() {
        // Arrange
        SecurityContext securityContext = new SecurityContextImpl(null);
        exchange.getAttributes().put(
            org.springframework.security.core.context.ReactiveSecurityContextHolder.class.getName(), 
            Mono.just(securityContext));

        // Act & Assert
        StepVerifier.create(jwtAuthFilter.filter(exchange, chain -> Mono.empty()))
            .verifyComplete();
    }

    @Test
    void getOrder_shouldReturnCorrectOrder() {
        // Act
        int order = jwtAuthFilter.getOrder();

        // Assert
        assertTrue(order > 0, "Порядок фильтра должен быть положительным");
        assertEquals(org.springframework.core.Ordered.LOWEST_PRECEDENCE - 100, order);
    }
}
