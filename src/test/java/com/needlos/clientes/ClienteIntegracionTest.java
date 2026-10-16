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
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Clientes: aislamiento multi-tenant (Reglas §9.6), contrato de listados (§5.5, §5.6), permisos,
 * telefono obligatorio y unico, filtros y bloqueo optimista con If-Match.
 */
class ClienteIntegracionTest extends IntegracionTest {

    private static final String CLIENTE =
            """
            {"nombre": "Laura", "apellido": "Gomez", "telefono": "3001234567"}""";

    @Test
    void otraSastreria_noVeNiModificaMisClientes_recibe404() throws Exception {
        String tokenA = admin();
        String tokenB = admin();
        String id =
                JsonPath.read(
                        crear(tokenA, CLIENTE).andReturn().getResponse().getContentAsString(),
                        "$.id");

        mvc.perform(get("/api/v1/clientes/" + id).header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLIENTE_NO_ENCONTRADO"));
        editar(tokenB, id, "0", CLIENTE).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/clientes/" + id).header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/clientes").header("Authorization", bearer(tokenB)))
                .andExpect(jsonPath("$.contenido", hasSize(0)));

        mvc.perform(get("/api/v1/clientes/" + id).header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk());
    }

    @Test
    void listado_usaPaginaEstandarConMaximoDe100() throws Exception {
        mvc.perform(get("/api/v1/clientes?size=500").header("Authorization", bearer(admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamano").value(100))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.totalElementos").value(0))
                .andExpect(jsonPath("$.contenido").isArray());
    }

    @Test
    void ordenarPorCampoNoPermitido_responde400() throws Exception {
        String token = admin();
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

    @Test
    void buscaPorNombreApellidoOTelefono() throws Exception {
        String token = admin();
        crear(
                        token,
                        """
                {"nombre": "Laura", "apellido": "Gomez", "telefono": "3001234567"}""")
                .andExpect(status().isCreated());
        crear(
                        token,
                        """
                {"nombre": "Pedro", "apellido": "Ruiz", "telefono": "3119998877"}""")
                .andExpect(status().isCreated());

        buscar(token, "gom").andExpect(jsonPath("$.contenido", hasSize(1)));
        buscar(token, "PEDRO").andExpect(jsonPath("$.contenido", hasSize(1)));
        buscar(token, "999887").andExpect(jsonPath("$.contenido[0].nombre").value("Pedro"));
    }

    @Test
    void elTelefonoEsObligatorio() throws Exception {
        crear(
                        admin(),
                        """
                {"nombre": "Laura", "apellido": "Gomez"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"));
    }

    @Test
    void elTelefonoEsUnicoPorSastreria_alCrearYAlEditar() throws Exception {
        String token = admin();
        crear(token, CLIENTE).andExpect(status().isCreated());
        crear(
                        token,
                        """
                {"nombre": "Marta", "apellido": "Gomez", "telefono": "3001234567"}""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TELEFONO_EN_USO"));

        String otro =
                crear(
                                token,
                                """
                        {"nombre": "Pedro", "apellido": "Ruiz", "telefono": "3119998877"}""")
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String idOtro = JsonPath.read(otro, "$.id");
        editar(
                        token,
                        idOtro,
                        "0",
                        """
                {"nombre": "Pedro", "apellido": "Ruiz", "telefono": "3001234567"}""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TELEFONO_EN_USO"));
        // Cambiarse a un numero libre si se puede.
        editar(
                        token,
                        idOtro,
                        "0",
                        """
                {"nombre": "Pedro", "apellido": "Ruiz", "telefono": "3220001111"}""")
                .andExpect(status().isOk());

        // Otra sastreria puede tener un cliente con el mismo numero.
        crear(admin(), CLIENTE).andExpect(status().isCreated());
    }

    @Test
    void eliminarUnCliente_liberaSuTelefono() throws Exception {
        String token = admin();
        String id =
                JsonPath.read(
                        crear(token, CLIENTE).andReturn().getResponse().getContentAsString(),
                        "$.id");
        mvc.perform(delete("/api/v1/clientes/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        crear(token, CLIENTE).andExpect(status().isCreated());
    }

    @Test
    void porTelefono_devuelveAlDuenoO404() throws Exception {
        String token = admin();
        crear(token, CLIENTE).andExpect(status().isCreated());

        porTelefono(token, "3001234567")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Laura"));
        porTelefono(token, "3000000000")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLIENTE_NO_ENCONTRADO"));
        // Aislado por sastreria.
        porTelefono(admin(), "3001234567").andExpect(status().isNotFound());
    }

    @Test
    void filtraPorFechaDeRegistro() throws Exception {
        String token = admin();
        crear(token, CLIENTE).andExpect(status().isCreated());
        String hoy = LocalDate.now().toString();
        String manana = LocalDate.now().plusDays(1).toString();

        filtrar(token, hoy, hoy).andExpect(jsonPath("$.contenido", hasSize(1)));
        filtrar(token, manana, null).andExpect(jsonPath("$.contenido", hasSize(0)));
        filtrar(token, null, LocalDate.now().minusDays(1).toString())
                .andExpect(jsonPath("$.contenido", hasSize(0)));
        filtrar(token, manana, hoy)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"));
        filtrar(token, "15/10/2026", null).andExpect(status().isBadRequest());
    }

    @Test
    void editar_exigeIfMatchYRechazaVersionVieja() throws Exception {
        String token = admin();
        String creado = crear(token, CLIENTE).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(creado, "$.id");
        Integer version = JsonPath.read(creado, "$.version");

        String editado =
                """
                {"nombre": "Laura", "apellido": "Gómez", "telefono": "3001234567"}""";
        editar(token, id, null, editado)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VERSION_REQUERIDA"));

        editar(token, id, "\"" + version + "\"", editado)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apellido").value("Gómez"))
                .andExpect(jsonPath("$.version").value(version + 1));

        // Otra persona edita con la version que ya quedo vieja: no pisa los cambios.
        editar(token, id, String.valueOf(version), editado)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICTO_CONCURRENCIA"));
    }

    @Test
    void unSastreCreaYEditaPeroNoElimina() throws Exception {
        UUID tenant = datos.crearSastreria();
        Cuenta dueno = datos.crearDueno(tenant);
        Cuenta sastre = datos.crearCuenta();
        datos.darAcceso(sastre, tenant, "SASTRE");
        String tokenSastre = iniciarSesion(sastre.getEmail()).accessToken();
        String tokenAdmin = iniciarSesion(dueno.getEmail()).accessToken();

        String creado =
                crear(tokenSastre, CLIENTE)
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String id = JsonPath.read(creado, "$.id");
        editar(tokenSastre, id, "0", CLIENTE).andExpect(status().isOk());

        mvc.perform(delete("/api/v1/clientes/" + id).header("Authorization", bearer(tokenSastre)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/clientes/" + id).header("Authorization", bearer(tokenAdmin)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/clientes/" + id).header("Authorization", bearer(tokenAdmin)))
                .andExpect(status().isNotFound());
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private String admin() throws Exception {
        return iniciarSesion(datos.crearDueno(datos.crearSastreria()).getEmail()).accessToken();
    }

    private ResultActions crear(String token, String json) throws Exception {
        return mvc.perform(
                post("/api/v1/clientes")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json));
    }

    private ResultActions editar(String token, String id, String ifMatch, String json)
            throws Exception {
        var peticion =
                put("/api/v1/clientes/" + id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json);
        if (ifMatch != null) {
            peticion.header("If-Match", ifMatch);
        }
        return mvc.perform(peticion);
    }

    private ResultActions buscar(String token, String texto) throws Exception {
        return mvc.perform(
                get("/api/v1/clientes")
                        .param("buscar", texto)
                        .header("Authorization", bearer(token)));
    }

    private ResultActions porTelefono(String token, String telefono) throws Exception {
        return mvc.perform(
                get("/api/v1/clientes/por-telefono/" + telefono)
                        .header("Authorization", bearer(token)));
    }

    private ResultActions filtrar(String token, String desde, String hasta) throws Exception {
        var peticion = get("/api/v1/clientes").header("Authorization", bearer(token));
        if (desde != null) {
            peticion.param("desde", desde);
        }
        if (hasta != null) {
            peticion.param("hasta", hasta);
        }
        return mvc.perform(peticion);
    }
}
