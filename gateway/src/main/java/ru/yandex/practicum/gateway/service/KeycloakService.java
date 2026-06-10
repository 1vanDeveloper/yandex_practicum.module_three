package ru.yandex.practicum.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import ru.yandex.practicum.gateway.dto.RegisterRequest;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakService {

    private final RestTemplate loadBalancedRestTemplate;
    private final RestTemplate keycloakRestTemplate = new RestTemplate();

    @Value("${keycloak.admin-server-url:http://localhost:8180}")
    private String keycloakServerUrl;

    @Value("${keycloak.admin-realm:master}")
    private String adminRealm;

    @Value("${keycloak.admin-client-id:admin-cli}")
    private String adminClientId;

    @Value("${keycloak.admin-username:admin}")
    private String adminUsername;

    @Value("${keycloak.admin-password:admin}")
    private String adminPassword;

    @Value("${keycloak.target-realm:bank}")
    private String targetRealm;

    /**
     * Создает пользователя в Keycloak после успешной регистрации в accounts-service.
     */
    public void createUserInKeycloak(RegisterRequest request) {
        log.info("Creating user in Keycloak: {}", request.getLogin());

        try {
            // Получаем admin token
            String adminToken = getAdminToken();

            // Создаем пользователя
            createUser(adminToken, request);

            log.info("Successfully created user in Keycloak: {}", request.getLogin());

        } catch (Exception e) {
            log.error("Error creating user in Keycloak: {}", request.getLogin(), e);
            throw new RuntimeException("Failed to create user in Keycloak", e);
        }
    }

    private String getAdminToken() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", adminClientId);
        formData.add("username", adminUsername);
        formData.add("password", adminPassword);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token",
                keycloakServerUrl, adminRealm);

        ResponseEntity<Map> response = keycloakRestTemplate.postForEntity(tokenUrl, request, Map.class);

        if (response.getBody() == null || !response.getBody().containsKey("access_token")) {
            throw new RuntimeException("Failed to obtain admin token from Keycloak");
        }

        return (String) response.getBody().get("access_token");
    }

    private void createUser(String adminToken, RegisterRequest request) {
        String createUserUrl = String.format("%s/admin/realms/%s/users", 
                keycloakServerUrl, targetRealm);

        Map<String, Object> userRepresentation = Map.of(
                "username", request.getLogin(),
                "email", request.getLogin() + "@bank.local",
                "firstName", request.getFirstName(),
                "lastName", request.getLastName(),
                "enabled", true,
                "emailVerified", false,
                "credentials", List.of(Map.of(
                        "type", "password",
                        "value", request.getPassword(),
                        "temporary", false
                ))
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(userRepresentation, headers);

        ResponseEntity<Void> response = keycloakRestTemplate.postForEntity(createUserUrl, entity, Void.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to create user in Keycloak: " + response.getStatusCode());
        }
    }
}
