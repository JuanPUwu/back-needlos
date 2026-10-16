package com.needlos.tenant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.needlos.security.cuenta.Cuenta;
import com.needlos.soporte.IntegracionTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class SastreriaIntegracionTest extends IntegracionTest {

    @Test
    void elAdminVeYActualizaLosDatosDeSuSastreria() throws Exception {
        String admin = adminConSastreria();

        mvc.perform(get("/api/v1/sastreria").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").isNotEmpty())
                .andExpect(jsonPath("$.plan").value("DEMO"));

        mvc.perform(
                        patch("/api/v1/sastreria")
                                .header("Authorization", bearer(admin))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"nombre":"El Buen Corte","nit":"900123","direccion":"Calle 1","telefono":"3001112233"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("El Buen Corte"))
                .andExpect(jsonPath("$.nit").value("900123"));

        mvc.perform(get("/api/v1/sastreria").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.direccion").value("Calle 1"))
                .andExpect(jsonPath("$.telefono").value("3001112233"));
    }

    @Test
    void elNombreEsObligatorio() throws Exception {
        String admin = adminConSastreria();
        mvc.perform(
                        patch("/api/v1/sastreria")
                                .header("Authorization", bearer(admin))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"nombre":"  "}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"));
    }

    @Test
    void unSastreNoPuedeVerNiEditarLaSastreria() throws Exception {
        UUID tenant = datos.crearSastreria();
        datos.crearDueno(tenant);
        Cuenta sastre = datos.crearCuenta();
        datos.darAcceso(sastre, tenant, "SASTRE");
        String token = iniciarSesion(sastre.getEmail()).accessToken();

        mvc.perform(get("/api/v1/sastreria").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    private String adminConSastreria() throws Exception {
        Cuenta dueno = datos.crearDueno(datos.crearSastreria());
        return iniciarSesion(dueno.getEmail()).accessToken();
    }
}
