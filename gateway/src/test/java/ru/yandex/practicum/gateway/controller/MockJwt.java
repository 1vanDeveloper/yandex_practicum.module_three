package ru.yandex.practicum.gateway.controller;

import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

class MockJwt extends Jwt {

    private final String preferredUsername;

    MockJwt(String preferredUsername) {
        super(
                "test-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Collections.singletonMap("alg", "HS256"),
                Collections.singletonMap("preferred_username", preferredUsername)
        );
        this.preferredUsername = preferredUsername;
    }

    @Override
    public String getClaimAsString(String claim) {
        if ("preferred_username".equals(claim)) {
            return preferredUsername;
        }
        return super.getClaimAsString(claim);
    }
}
