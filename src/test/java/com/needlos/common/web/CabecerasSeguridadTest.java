package com.needlos.common.web;

import com.needlos.soporte.IntegracionTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Reglas §8.3 (cabeceras de seguridad) y §13 (id de correlacion). */
class CabecerasSeguridadTest extends IntegracionTest {

    @Test
    void respuestas_llevanCabecerasDeSeguridadEIdDeCorrelacion() throws Exception {
        mvc.perform(get("/actuator/health").secure(true))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'none'")))
                .andExpect(header().string("Strict-Transport-Security", containsString("max-age=31536000")))
                .andExpect(header().exists(CorrelationIdFilter.CABECERA));
    }

    @Test
    void idDeCorrelacionValidoDelCliente_seReutiliza() throws Exception {
        mvc.perform(get("/actuator/health").header(CorrelationIdFilter.CABECERA, "soporte-1234"))
                .andExpect(header().string(CorrelationIdFilter.CABECERA, "soporte-1234"));
    }

    @Test
    void swagger_noEstaExpuestoFueraDeDev() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
    }
}
