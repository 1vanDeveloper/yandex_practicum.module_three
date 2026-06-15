package ru.yandex.practicum.transfer.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.yandex.practicum.transfer.config.TestSecurityConfig;
import ru.yandex.practicum.transfer.dto.TransferRequest;
import ru.yandex.practicum.transfer.dto.TransferResponse;
import ru.yandex.practicum.transfer.entity.TransferStatus;
import ru.yandex.practicum.transfer.service.TransferService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = {TestSecurityConfig.class})
class TransferControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private TransferService transferService;

    private MockMvc mockMvc;
    private Jwt mockJwt;

    @BeforeEach
    void setUp() {
        reset(transferService);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Setup mock JWT
        mockJwt = Jwt.withTokenValue("test-token")
            .header("alg", "none")
            .header("typ", "JWT")
            .claim("preferred_username", "test_user")
            .subject("test_user")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
    }

    @Test
    void processTransfer_whenValidRequest_returnsOk() throws Exception {
        // Setup SecurityContext
        JwtAuthenticationToken auth = new JwtAuthenticationToken(
            mockJwt,
            AuthorityUtils.createAuthorityList("ROLE_USER"),
            "test_user"
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        
        try {
            // Given
            TransferResponse response = new TransferResponse(
                    1L, "test_user", "receiver", new BigDecimal("100.00"),
                    TransferStatus.COMPLETED, null, null, null
            );

            when(transferService.createTransfer(any(TransferRequest.class)))
                    .thenReturn(CompletableFuture.completedFuture(response));

            // When & Then
            mockMvc.perform(post("/transfer")
                            .param("value", "100")
                            .param("login", "receiver"))
                    .andExpect(status().isOk());

            verify(transferService).createTransfer(any(TransferRequest.class));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
