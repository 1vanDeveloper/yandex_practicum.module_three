package ru.yandex.practicum.mybankfront.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class JwtSessionFilter extends OncePerRequestFilter {

    private final JwtDecoder jwtDecoder;

    public JwtSessionFilter(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session != null) {
            String token = (String) session.getAttribute("jwt_token");
            if (token != null) {
                try {
                    Jwt jwt = jwtDecoder.decode(token);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(jwt, null, java.util.Collections.emptyList());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("JWT token from session loaded for user: {}", jwt.getSubject());
                } catch (Exception e) {
                    log.debug("Failed to decode JWT from session: {}", e.getMessage());
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
