package com.needlos.clientes;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.needlos.security.cuenta.Cuenta;
import com.needlos.soporte.IntegracionTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Aislamiento multi-tenant (Reglas §9.6) y contrato de listados (Reglas §5.5, §5.6). */
class ClienteIntegracionTest extends IntegracionTest {

    private static final String CLIENTE =
            """
            {"nombre": "Laura", "apellido": "Gomez", "telefono": "3001234567"}""";

    @Test
    void otraSastreria_noVeNiModificaMisClientes_recibe404() throws Exception {
        String tokenA =
                iniciarSesion(datos.crearDueno(datos.crearSastreria()).getEmail()).accessToken();
        String tokenB =
                iniciarSesion(datos.crearDueno(datos.crearSastreria()).getEmail()).accessToken();

        String body =
                mvc.perform(
                                post("/api/v1/clientes")
                                        .header("Authorization", bearer(tokenA))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(CLIENTE))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String id = JsonPath.read(body, "$.id");

        mvc.perform(get("/api/v1/clientes/" + id).header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLIENTE_NO_ENCONTRADO"));
        mvc.perform(
                        put("/api/v1/clientes/" + id)
                                .header("Authorization", bearer(tokenB))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CLIENTE))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/clientes/" + id).header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/clientes").header("Authorization", bearer(tokenB)))
                .andExpect(jsonPath("$.contenido", hasSize(0)));

        mvc.perform(get("/api/v1/clientes/" + id).header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk());
    }

    @Test
    void listado_usaPaginaEstandarConMaximoDe100() throws Exception {
        Cuenta dueno = datos.crearDueno(datos.crearSastreria());
        String token = iniciarSesion(dueno.getEmail()).accessToken();

        mvc.perform(get("/api/v1/clientes?size=500").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamano").value(100))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.totalElementos").value(0))
                .andExpect(jsonPath("$.contenido").isArray());
    }

    @Test
    void ordenarPorCampoNoPermitido_responde400() throws Exception {
        String token =
                iniciarSesion(datos.crearDueno(datos.crearSastreria()).getEmail()).accessToken();

        mvc.perform(
                        get("/api/v1/clientes?sort=telefono,asc")
                                .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ORDENAMIENTO_NO_PERMITIDO"));
        mvc.perform(
                        get("/api/v1/clientes?sort=apellido,desc")
                                .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }
}
