package ru.yandex.practicum.accounts;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;

@TestConfiguration
@ConditionalOnProperty(name = "spring.security.enabled", havingValue = "false")
public class TestSecurityConfig {

    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )
            .addFilterBefore(new JwtTestFilter(), org.springframework.security.web.authentication.AnonymousAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Primary
    public JwtDecoder testJwtDecoder() {
        return token -> createTestJwt();
    }

    private Jwt createTestJwt() {
        return Jwt.withTokenValue("test-token")
            .header("alg", "none")
            .header("typ", "JWT")
            .claim("preferred_username", "test_user")
            .claim("sub", "test_user")
            .claim("iat", Instant.now().getEpochSecond())
            .subject("test_user")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
    }

    static class JwtTestFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                       FilterChain filterChain) throws ServletException, IOException {
            Jwt jwt = createTestJwt();
            JwtAuthenticationToken auth = new JwtAuthenticationToken(
                jwt,
                AuthorityUtils.createAuthorityList("ROLE_USER"),
                "test_user"
            );
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            try {
                filterChain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clearContext();
            }
        }

        private static Jwt createTestJwt() {
            return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .header("typ", "JWT")
                .claim("preferred_username", "test_user")
                .claim("sub", "test_user")
                .claim("iat", Instant.now().getEpochSecond())
                .subject("test_user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        }
    }
}
