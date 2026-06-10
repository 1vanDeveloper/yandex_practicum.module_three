package ru.yandex.practicum.mybankfront;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.security.oauth2.client.registration.keycloak.enabled=false",
    "spring.security.oauth2.client.registration.frontend-service.enabled=false"
})
class FrontendApplicationTests {

	@Test
	void contextLoads() {
	}

}
