package ru.yandex.practicum.accounts.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret:mySecretKeyForJWTTokenGenerationMustBeLongEnough}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    private Key getSigningKey() {
        byte[] keyBytes = secret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractLogin(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractPrivileges(String token) {
        return extractClaim(token, claims -> claims.get("privileges", List.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Генерирует JWT-токен для пользователя с базовыми привилегиями.
     */
    public String generateToken(String login) {
        List<String> privileges = getDefaultPrivileges(login);
        return generateToken(login, privileges);
    }

    /**
     * Генерирует JWT-токен для пользователя с указанными привилегиями.
     *
     * @param login логин пользователя
     * @param privileges список привилегий (например, "accounts:read", "cash:write", "transfer:write")
     */
    public String generateToken(String login, List<String> privileges) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("privileges", privileges);
        return createToken(claims, login);
    }

    /**
     * Возвращает привилегии по умолчанию для пользователя.
     * Можно расширить для загрузки из БД или конфигурации.
     */
    private List<String> getDefaultPrivileges(String login) {
        // Администратор имеет доступ ко всем сервисам
        if ("admin".equals(login)) {
            return List.of("accounts:read", "accounts:write", "cash:write", "transfer:write");
        }
        // Обычный пользователь имеет доступ к базовым операциям
        return List.of("accounts:read", "accounts:write", "cash:write", "transfer:write");
    }

    /**
     * Извлекает привилегии для аккаунта.
     * Используется при генерации токена для включения привилегий в JWT.
     *
     * @param account аккаунт пользователя
     * @return список привилегий
     */
    public List<String> extractPrivilegesFromAccount(ru.yandex.practicum.accounts.entity.Account account) {
        return getDefaultPrivileges(account.getLogin());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Boolean validateToken(String token, String login) {
        final String extractedLogin = extractLogin(token);
        return (extractedLogin.equals(login) && !isTokenExpired(token));
    }
}
