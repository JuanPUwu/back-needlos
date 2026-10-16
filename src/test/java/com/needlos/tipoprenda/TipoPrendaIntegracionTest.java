package com.needlos.tipoprenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.needlos.common.correo.Correo;
import com.needlos.common.correo.EnviadorCorreo;
import com.needlos.security.cuenta.Cuenta;
import com.needlos.soporte.IntegracionTest;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Catalogo de tipos de prenda: catalogo base, nombre unico, If-Match, permisos y precio sugerido.
 */
class TipoPrendaIntegracionTest extends IntegracionTest {

    private static final Pattern CODIGO = Pattern.compile("\\b(\\d{6})\\b");

    @MockitoBean private EnviadorCorreo enviador;

    @Test
    void unaSastreriaRegistradaArrancaConElCatalogoBase() throws Exception {
        String email = "nueva-" + UUID.randomUUID() + "@prueba.com";
        mvc.perform(
                        post("/api/v1/auth/registrar-sastreria")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"nombreSastreria": "Sastreria Nueva", "nombreAdmin": "Ana", "apellidoAdmin": "Perez",
                                         "numeroDocumento": "123", "email": "%s", "password": "Clave123!"}"""
                                                .formatted(email)))
                .andExpect(status().isCreated());
        mvc.perform(
                        post("/api/v1/auth/verificar-correo")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"email": "%s", "codigo": "%s"}"""
                                                .formatted(email, codigoRecibido())))
                .andExpect(status().isOk());
        String token = iniciarSesion(email).accessToken();

        String cuerpo = listar(token, false).andReturn().getResponse().getContentAsString();
        List<String> nombres = JsonPath.read(cuerpo, "$[*].nombre");
        assertThat(nombres)
                .containsExactlyInAnyOrder("Camisa", "Pantalón", "Saco", "Vestido", "Chaqueta");
        List<Object> precios = JsonPath.read(cuerpo, "$[*].precioBase");
        assertThat(precios).containsOnlyNulls();
    }

    @Test
    void crear_elNombreEsUnicoSinDistinguirMayusculas() throws Exception {
        String token = admin();
        crear(token, "Abrigo", null).andExpect(status().isCreated());
        crear(token, "  abrigo ", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TIPO_PRENDA_DUPLICADO"));
    }

    @Test
    void editar_exigeIfMatchYNoPermiteRenombrarAUnNombreExistente() throws Exception {
        String token = admin();
        crear(token, "Abrigo", null).andExpect(status().isCreated());
        String creado =
                crear(token, "Gabán", "50000").andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(creado, "$.id");
        Integer version = JsonPath.read(creado, "$.version");

        editar(token, id, null, "Gabán largo", "55000")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VERSION_REQUERIDA"));
        editar(token, id, String.valueOf(version), "Abrigo", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TIPO_PRENDA_DUPLICADO"));
        editar(token, id, String.valueOf(version), "Gabán largo", "55000")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Gabán largo"))
                .andExpect(jsonPath("$.precioBase").value(55000));
        editar(token, id, String.valueOf(version), "Otro", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICTO_CONCURRENCIA"));
    }

    @Test
    void elPrecioBaseNoPuedeSerNegativo() throws Exception {
        crear(admin(), "Abrigo", "-1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"));
    }

    @Test
    void desactivar_loQuitaDeLaListaParaPedidosPeroNoLoBorra() throws Exception {
        String token = admin();
        String id =
                JsonPath.read(
                        crear(token, "Abrigo", null).andReturn().getResponse().getContentAsString(),
                        "$.id");

        mvc.perform(
                        patch("/api/v1/tipos-prenda/{id}/desactivar", id)
                                .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
        assertThat(listar(token, false).andReturn().getResponse().getContentAsString())
                .doesNotContain("Abrigo");
        assertThat(listar(token, true).andReturn().getResponse().getContentAsString())
                .contains("Abrigo");

        mvc.perform(
                        patch("/api/v1/tipos-prenda/{id}/reactivar", id)
                                .header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.activo").value(true));
        assertThat(listar(token, false).andReturn().getResponse().getContentAsString())
                .contains("Abrigo");
    }

    @Test
    void unSastreListaPeroNoGestionaElCatalogo() throws Exception {
        UUID tenant = datos.crearSastreria();
        datos.crearDueno(tenant);
        Cuenta sastre = datos.crearCuenta();
        datos.darAcceso(sastre, tenant, "SASTRE");
        String token = iniciarSesion(sastre.getEmail()).accessToken();

        listar(token, false).andExpect(status().isOk());
        crear(token, "Abrigo", null).andExpect(status().isForbidden());
    }

    @Test
    void otraSastreriaNoVeNiEditaMiCatalogo() throws Exception {
        String tokenA = admin();
        String tokenB = admin();
        String id =
                JsonPath.read(
                        crear(tokenA, "Abrigo", null)
                                .andReturn()
                                .getResponse()
                                .getContentAsString(),
                        "$.id");

        editar(tokenB, id, "0", "Mio", null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TIPO_PRENDA_NO_ENCONTRADO"));
        mvc.perform(
                        get("/api/v1/tipos-prenda/{id}/precio-sugerido", id)
                                .header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());
    }

    @Test
    void precioSugerido_ultimoCobradoSinAnulados_luegoPrecioBase_luegoNinguno() throws Exception {
        String token = admin();
        String sinPrecio =
                JsonPath.read(
                        crear(token, "Abrigo", null).andReturn().getResponse().getContentAsString(),
                        "$.id");
        String conBase =
                JsonPath.read(
                        crear(token, "Gabán", "40000")
                                .andReturn()
                                .getResponse()
                                .getContentAsString(),
                        "$.id");

        sugerido(token, sinPrecio)
                .andExpect(jsonPath("$.origen").value("NINGUNO"))
                .andExpect(jsonPath("$.precio").doesNotExist());
        sugerido(token, conBase)
                .andExpect(jsonPath("$.origen").value("PRECIO_BASE"))
                .andExpect(jsonPath("$.precio").value(40000));

        String cliente = crearCliente(token);
        crearPedido(token, cliente, conBase, "45000");
        String ultimo = crearPedido(token, cliente, conBase, "50000");
        sugerido(token, conBase)
                .andExpect(jsonPath("$.origen").value("HISTORICO"))
                .andExpect(jsonPath("$.precio").value(50000));

        // Un pedido anulado no cuenta: vuelve al ultimo precio vigente.
        mvc.perform(
                        patch("/api/v1/ordenes/{id}/anular", ultimo)
                                .header("Authorization", bearer(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"razon\": \"Precio equivocado\"}"))
                .andExpect(status().isOk());
        sugerido(token, conBase).andExpect(jsonPath("$.precio").value(45000));
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private String admin() throws Exception {
        return iniciarSesion(datos.crearDueno(datos.crearSastreria()).getEmail()).accessToken();
    }

    private ResultActions listar(String token, boolean todos) throws Exception {
        return mvc.perform(
                get("/api/v1/tipos-prenda")
                        .param("todos", String.valueOf(todos))
                        .header("Authorization", bearer(token)));
    }

    private ResultActions crear(String token, String nombre, String precio) throws Exception {
        return mvc.perform(
                post("/api/v1/tipos-prenda")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(nombre, precio)));
    }

    private ResultActions editar(
            String token, String id, String ifMatch, String nombre, String precio)
            throws Exception {
        var peticion =
                put("/api/v1/tipos-prenda/{id}", id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(nombre, precio));
        if (ifMatch != null) {
            peticion.header("If-Match", ifMatch);
        }
        return mvc.perform(peticion);
    }

    private ResultActions sugerido(String token, String id) throws Exception {
        return mvc.perform(
                get("/api/v1/tipos-prenda/{id}/precio-sugerido", id)
                        .header("Authorization", bearer(token)));
    }

    private String crearCliente(String token) throws Exception {
        String cuerpo =
                mvc.perform(
                                post("/api/v1/clientes")
                                        .header("Authorization", bearer(token))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"nombre": "Laura", "apellido": "Gomez", "telefono": "3001234567"}"""))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return JsonPath.read(cuerpo, "$.id");
    }

    private String crearPedido(String token, String cliente, String tipo, String precio)
            throws Exception {
        String cuerpo =
                mvc.perform(
                                post("/api/v1/ordenes")
                                        .header("Authorization", bearer(token))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"clienteId": "%s", "fechaEntrega": "%s", "prendas": [
                                                  {"tipoPrendaId": "%s", "cantidad": 1, "descripcion": "Ajuste de mangas", "precioUnitario": %s}]}"""
                                                        .formatted(
                                                                cliente,
                                                                LocalDate.now().plusDays(5),
                                                                tipo,
                                                                precio)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return JsonPath.read(cuerpo, "$.id");
    }

    private static String json(String nombre, String precio) {
        return precio == null
                ? "{\"nombre\": \"%s\"}".formatted(nombre)
                : "{\"nombre\": \"%s\", \"precioBase\": %s}".formatted(nombre, precio);
    }

    private String codigoRecibido() {
        ArgumentCaptor<Correo> captor = ArgumentCaptor.forClass(Correo.class);
        verify(enviador, timeout(5000)).enviar(captor.capture());
        Matcher matcher = CODIGO.matcher(captor.getValue().texto());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
