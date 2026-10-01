package com.needlos.security.filtro;

import com.needlos.soporte.IntegracionTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "needlos.rate-limit.auth-por-minuto=3")
class RateLimitIntegracionTest extends IntegracionTest {

    @Test
    void superarElLimiteDeLogin_responde429ConRetryAfter() throws Exception {
        for (int i = 0; i < 3; i++) {
            intentarLogin("10.0.0.1").andExpect(status().isUnauthorized());
        }

        intentarLogin("10.0.0.1")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("DEMASIADAS_SOLICITUDES"));

        // Otra IP no se ve afectada.
        intentarLogin("10.0.0.2").andExpect(status().isUnauthorized());
    }

    private ResultActions intentarLogin(String ip) throws Exception {
        return mvc.perform(post("/api/v1/auth/login")
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "nadie@prueba.com", "password": "Clave123!"}"""));
    }
}
