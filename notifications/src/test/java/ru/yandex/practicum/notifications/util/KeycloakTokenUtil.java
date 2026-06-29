package ru.yandex.practicum.notifications.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Utility class for obtaining JWT tokens from Keycloak for integration tests.
 * Requires Keycloak to be running in Kubernetes with port-forward:
 *   kubectl port-forward svc/keycloak 8180:8080
 */
public class KeycloakTokenUtil {

    private static final String KEYCLOAK_URL = "http://localhost:8180";
    private static final String REALM = "bank";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private KeycloakTokenUtil() {
        // Utility class
    }

    /**
     * Получает access токен для пользователя через password grant type.
     * Использует admin-cli client для direct access grants.
     *
     * @param username имя пользователя
     * @param password пароль пользователя
     * @return access токен
     * @throws RuntimeException если не удалось получить токен
     */
    public static String getUserToken(String username, String password) {
        try {
            URL url = new URL(KEYCLOAK_URL + "/realms/" + REALM + "/protocol/openid-connect/token");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);

            // Используем admin-cli client который поддерживает direct access grants
            String body = "grant_type=password&client_id=admin-cli" +
                "&username=" + username + "&password=" + password;

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            if (status != 200) {
                throw new RuntimeException("Failed to get token: HTTP " + status);
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }

                JsonNode jsonNode = objectMapper.readTree(response.toString());
                return jsonNode.get("access_token").asText();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to obtain user token: " + e.getMessage(), e);
        }
    }

    /**
     * Получает service account токен для клиента через client credentials grant type.
     * 
     * @param clientId ID клиента
     * @param clientSecret секрет клиента
     * @return access токен
     * @throws RuntimeException если не удалось получить токен
     */
    public static String getClientToken(String clientId, String clientSecret) {
        try {
            URL url = new URL(KEYCLOAK_URL + "/realms/" + REALM + "/protocol/openid-connect/token");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);

            String body = "grant_type=client_credentials&client_id=" + clientId + "&client_secret=" + clientSecret;

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            if (status != 200) {
                throw new RuntimeException("Failed to get client token: HTTP " + status);
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                
                JsonNode jsonNode = objectMapper.readTree(response.toString());
                return jsonNode.get("access_token").asText();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to obtain client token: " + e.getMessage(), e);
        }
    }

    /**
     * Создаёт заголовок Authorization с Bearer токеном.
     * 
     * @param token access токен
     * @return Map с заголовком Authorization
     */
    public static Map<String, String> createAuthHeader(String token) {
        return Map.of("Authorization", "Bearer " + token);
    }
}
