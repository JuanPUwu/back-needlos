package com.needlos.pedidos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.needlos.security.acceso.Acceso;
import com.needlos.security.acceso.Permiso;
import com.needlos.security.cuenta.Cuenta;
import com.needlos.soporte.IntegracionTest;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Pedidos y abonos (F4 + F5): creacion con cliente nuevo o existente en una transaccion,
 * idempotencia, numeracion #G-L, permisos por accion, transiciones de estado, edicion, anulacion,
 * filtros, ticket y aislamiento entre sastrerias.
 */
class PedidoIntegracionTest extends IntegracionTest {

    @Autowired private JdbcTemplate jdbc;

    private UUID tenant;
    private String admin;
    private String tipo;
    private String manana;

    @BeforeEach
    void preparar() throws Exception {
        tenant = datos.crearSastreria();
        admin = iniciarSesion(datos.crearDueno(tenant).getEmail()).accessToken();
        tipo = crearTipo(admin, "Camisa");
        manana = LocalDate.now().plusDays(1).toString();
    }

    // ── Crear ───────────────────────────────────────────────────────

    @Test
    void crearConClienteNuevo_creaClienteYPedidoConNumeroGlobalYLocal() throws Exception {
        String pedido =
                crearPedido(admin, clienteNuevo("3001234567"), prenda(tipo, 2, "20000"), null, null)
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.numero", matchesPattern("^#\\d+-1$")))
                        .andExpect(jsonPath("$.numeroLocal").value(1))
                        .andExpect(jsonPath("$.estado").value("EN_PROCESO"))
                        .andExpect(jsonPath("$.subtotal").value(40000))
                        .andExpect(jsonPath("$.total").value(40000))
                        .andExpect(jsonPath("$.saldo").value(40000))
                        .andExpect(jsonPath("$.pagado").value(false))
                        .andExpect(jsonPath("$.cliente.nombre").value("Ana"))
                        .andExpect(jsonPath("$.prendas[0].tipoPrenda").value("Camisa"))
                        .andExpect(jsonPath("$.prendas[0].historial", hasSize(1)))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        porTelefono(admin, "3001234567").andExpect(status().isOk());
        String segundo =
                cuerpo(
                        crearPedido(
                                admin,
                                clienteExistente(clienteId(pedido)),
                                prenda(tipo, 1, "10000"),
                                null,
                                null));
        assertThat((Integer) JsonPath.read(segundo, "$.numeroLocal")).isEqualTo(2);
        long g1 = ((Number) JsonPath.read(pedido, "$.numeroGlobal")).longValue();
        long g2 = ((Number) JsonPath.read(segundo, "$.numeroGlobal")).longValue();
        assertThat(g2).isGreaterThan(g1);
    }

    @Test
    void siFallaElPedido_noQuedaElClienteNuevo() throws Exception {
        String inactivo = crearTipo(admin, "Abrigo");
        mvc.perform(
                        patch("/api/v1/tipos-prenda/{id}/desactivar", inactivo)
                                .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());

        crearPedido(admin, clienteNuevo("3109998877"), prenda(inactivo, 1, "10000"), null, null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("TIPO_PRENDA_INACTIVO"));
        porTelefono(admin, "3109998877").andExpect(status().isNotFound());
    }

    @Test
    void idempotencia_exigeClaveYNoDuplicaAlRepetir() throws Exception {
        String json =
                cuerpoPedido(clienteNuevo("3001234567"), prenda(tipo, 1, "10000"), null, null);
        mvc.perform(
                        post("/api/v1/pedidos")
                                .header("Authorization", bearer(admin))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCIA_REQUERIDA"));

        String clave = UUID.randomUUID().toString();
        String primero = cuerpo(enviarPedido(admin, json, clave));
        String repetido = cuerpo(enviarPedido(admin, json, clave));
        assertThat((String) JsonPath.read(repetido, "$.id"))
                .isEqualTo(JsonPath.read(primero, "$.id"));
        listar(admin, "").andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void validaCliente_fechaYDescuento() throws Exception {
        crearPedido(admin, "", prenda(tipo, 1, "10000"), null, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLIENTE_REQUERIDO"));
        crearPedido(
                        admin,
                        clienteNuevo("3001234567"),
                        prenda(tipo, 1, "10000"),
                        null,
                        null,
                        LocalDate.now().minusDays(1).toString())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("FECHA_ENTREGA_INVALIDA"));
        crearPedido(admin, clienteNuevo("3001234567"), prenda(tipo, 1, "10000"), "10001", null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("DESCUENTO_INVALIDO"));
        // Hoy si se acepta como fecha de entrega.
        crearPedido(
                        admin,
                        clienteNuevo("3001234567"),
                        prenda(tipo, 1, "10000"),
                        "1000",
                        null,
                        LocalDate.now().toString())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(9000));
    }

    // ── Abonos ──────────────────────────────────────────────────────

    @Test
    void abonos_nuncaSuperanElSaldoYElPedidoQuedaPagadoSolo() throws Exception {
        String pedido =
                cuerpo(
                        crearPedido(
                                admin,
                                clienteNuevo("3001234567"),
                                prenda(tipo, 1, "50000"),
                                null,
                                abono("20000", "NEQUI")));
        String id = JsonPath.read(pedido, "$.id");
        assertThat(((Number) JsonPath.read(pedido, "$.saldo")).intValue()).isEqualTo(30000);

        abonar(admin, id, "30001", UUID.randomUUID().toString())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ABONO_SUPERA_SALDO"));
        // El cliente debe lo mismo (en su ficha y en el listado de clientes).
        String cliente = clienteId(pedido);
        mvc.perform(get("/api/v1/clientes/{id}", cliente).header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.saldoPendiente").value(30000));
        mvc.perform(get("/api/v1/clientes").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.contenido[0].saldoPendiente").value(30000));

        String clave = UUID.randomUUID().toString();
        abonar(admin, id, "30000", clave)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saldo").value(0))
                .andExpect(jsonPath("$.pagado").value(true));
        // Reintento con la misma clave: no cobra dos veces.
        abonar(admin, id, "30000", clave).andExpect(jsonPath("$.abonos", hasSize(2)));
        mvc.perform(get("/api/v1/clientes/{id}", cliente).header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.saldoPendiente").value(0));
    }

    @Test
    void anularPedido_exigeQueNoTengaAbonosVigentes() throws Exception {
        String pedido =
                cuerpo(
                        crearPedido(
                                admin,
                                clienteNuevo("3001234567"),
                                prenda(tipo, 1, "50000"),
                                null,
                                abono("20000", "EFECTIVO")));
        String id = JsonPath.read(pedido, "$.id");
        String abonoId = JsonPath.read(pedido, "$.abonos[0].id");

        anular(admin, id, "Cliente desistió")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PEDIDO_CON_ABONOS"));

        mvc.perform(
                        patch("/api/v1/abonos/{id}/anular", abonoId)
                                .header("Authorization", bearer(admin))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"motivo": "DEVOLUCION", "razon": "Se le devolvió el dinero"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.abonado").value(0))
                .andExpect(jsonPath("$.abonos[0].anulado").value(true))
                .andExpect(jsonPath("$.abonos[0].motivoAnulacion").value("DEVOLUCION"));

        anular(admin, id, "Cliente desistió")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADO"));
        abonar(admin, id, "1000", UUID.randomUUID().toString())
                .andExpect(jsonPath("$.code").value("PEDIDO_ANULADO"));
    }

    // ── Estados ─────────────────────────────────────────────────────

    @Test
    void transicionesDeEstado_yEstadoDerivadoDelPedido() throws Exception {
        String pedido =
                cuerpo(
                        crearPedido(
                                admin,
                                clienteNuevo("3001234567"),
                                prenda(tipo, 1, "10000") + "," + prenda(tipo, 1, "20000"),
                                null,
                                null));
        String id = JsonPath.read(pedido, "$.id");
        String p1 = JsonPath.read(pedido, "$.prendas[0].id");
        String p2 = JsonPath.read(pedido, "$.prendas[1].id");

        estado(admin, id, p1, "ENTREGADO")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("TRANSICION_INVALIDA"));
        estado(admin, id, p1, "FINALIZADO").andExpect(jsonPath("$.estado").value("EN_PROCESO"));
        estado(admin, id, p1, "EN_PROCESO").andExpect(status().isOk());
        estado(admin, id, p1, "FINALIZADO").andExpect(status().isOk());
        estado(admin, id, p2, "FINALIZADO").andExpect(jsonPath("$.estado").value("FINALIZADO"));

        entregar(admin, id, List.of(p1, p2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENTREGADO"))
                .andExpect(jsonPath("$.prendas[0].historial", hasSize(5)));
        estado(admin, id, p1, "EN_PROCESO").andExpect(jsonPath("$.code").value("PRENDA_ENTREGADA"));
    }

    // ── Permisos ────────────────────────────────────────────────────

    @Test
    void unSastre_soloHaceLoQueElAdminLePermite() throws Exception {
        Cuenta cuenta = datos.crearCuenta();
        Acceso acceso = datos.darAcceso(cuenta, tenant, "SASTRE");
        String sastre = iniciarSesion(cuenta.getEmail()).accessToken();

        // Sin permisos: ve pedidos pero no crea.
        mvc.perform(get("/api/v1/permisos/mios").header("Authorization", bearer(sastre)))
                .andExpect(jsonPath("$.admin").value(false))
                .andExpect(jsonPath("$.permisos", hasSize(0)))
                .andExpect(jsonPath("$.accesoId").value(acceso.getId().toString()));
        crearPedido(sastre, clienteNuevo("3001234567"), prenda(tipo, 1, "10000"), null, null)
                .andExpect(status().isForbidden());
        listar(sastre, "").andExpect(status().isOk());

        datos.darPermisos(acceso, Permiso.CREAR_PEDIDOS, Permiso.CAMBIAR_ESTADO_PRENDAS);
        // Puede crear y poner el precio, pero no asignar sastre ni cobrar sin esos permisos.
        crearPedido(
                        sastre,
                        clienteNuevo("3001234567"),
                        prendaConSastre(tipo, acceso.getId().toString()),
                        null,
                        null)
                .andExpect(status().isForbidden());
        crearPedido(
                        sastre,
                        clienteNuevo("3001234567"),
                        prenda(tipo, 1, "10000"),
                        null,
                        abono("1000", "EFECTIVO"))
                .andExpect(status().isForbidden());
        String pedido =
                cuerpo(
                        crearPedido(
                                sastre,
                                clienteNuevo("3001234567"),
                                prenda(tipo, 1, "12345"),
                                null,
                                null));
        String id = JsonPath.read(pedido, "$.id");
        String prenda = JsonPath.read(pedido, "$.prendas[0].id");

        estado(sastre, id, prenda, "FINALIZADO").andExpect(status().isOk());
        estado(sastre, id, prenda, "ENTREGADO").andExpect(status().isForbidden());
        abonar(sastre, id, "1000", UUID.randomUUID().toString()).andExpect(status().isForbidden());
        anular(sastre, id, "Ya no lo quiere").andExpect(status().isForbidden());
        mvc.perform(
                        put("/api/v1/pedidos/{id}", id)
                                .header("Authorization", bearer(sastre))
                                .header("If-Match", "0")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(edicion(manana, null, prenda(tipo, 1, "1"))))
                .andExpect(status().isForbidden());

        datos.darPermisos(acceso, Permiso.ENTREGAR_PRENDAS);
        estado(sastre, id, prenda, "ENTREGADO").andExpect(status().isOk());
    }

    @Test
    void elAdminAsignaPermisosDesdeEquipo() throws Exception {
        Cuenta cuenta = datos.crearCuenta();
        Acceso acceso = datos.darAcceso(cuenta, tenant, "SASTRE");
        mvc.perform(
                        put("/api/v1/empleados/{id}", acceso.getId())
                                .header("Authorization", bearer(admin))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"nombre": "Luis", "apellido": "Gil", "rol": "SASTRE",
                                         "permisos": ["CREAR_PEDIDOS", "REGISTRAR_ABONOS"]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permisos", hasSize(2)));

        String sastre = iniciarSesion(cuenta.getEmail()).accessToken();
        mvc.perform(get("/api/v1/permisos/mios").header("Authorization", bearer(sastre)))
                .andExpect(jsonPath("$.permisos[0]").value("CREAR_PEDIDOS"))
                .andExpect(jsonPath("$.permisos[1]").value("REGISTRAR_ABONOS"));
        mvc.perform(get("/api/v1/permisos/mios").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.admin").value(true))
                .andExpect(jsonPath("$.permisos", hasSize(Permiso.values().length)));
        mvc.perform(get("/api/v1/empleados/asignables").header("Authorization", bearer(sastre)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // ── Editar ──────────────────────────────────────────────────────

    @Test
    void editar_actualizaAgregaYQuitaPrendasNoEntregadas() throws Exception {
        String pedido =
                cuerpo(
                        crearPedido(
                                admin,
                                clienteNuevo("3001234567"),
                                prenda(tipo, 1, "10000") + "," + prenda(tipo, 1, "20000"),
                                null,
                                abono("15000", "TARJETA")));
        String id = JsonPath.read(pedido, "$.id");
        String p1 = JsonPath.read(pedido, "$.prendas[0].id");
        String p2 = JsonPath.read(pedido, "$.prendas[1].id");

        editar(admin, id, null, edicion(manana, null, prendaConId(p1, tipo, "10000")))
                .andExpect(jsonPath("$.code").value("VERSION_REQUERIDA"));
        // Quitar p2 dejaria el total (10000) por debajo de lo abonado (15000).
        editar(admin, id, "0", edicion(manana, null, prendaConId(p1, tipo, "10000")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("TOTAL_MENOR_QUE_ABONADO"));

        editar(
                        admin,
                        id,
                        "0",
                        edicion(
                                LocalDate.now().plusDays(3).toString(),
                                "500",
                                prendaConId(p1, tipo, "12000") + "," + prenda(tipo, 2, "5000")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prendas", hasSize(2)))
                .andExpect(jsonPath("$.subtotal").value(22000))
                .andExpect(jsonPath("$.total").value(21500))
                .andExpect(jsonPath("$.saldo").value(6500));

        // p2 ya no esta en el pedido.
        estado(admin, id, p2, "FINALIZADO").andExpect(status().isNotFound());

        estado(admin, id, p1, "FINALIZADO");
        entregar(admin, id, List.of(p1)).andExpect(status().isOk());
        String actual =
                cuerpo(
                        mvc.perform(
                                get("/api/v1/pedidos/{id}", id)
                                        .header("Authorization", bearer(admin))));
        String version = String.valueOf((Integer) JsonPath.read(actual, "$.version"));
        String nueva = JsonPath.read(actual, "$.prendas[1].id");
        editar(admin, id, version, edicion(manana, null, prendaConId(nueva, tipo, "5000")))
                .andExpect(jsonPath("$.code").value("PRENDA_ENTREGADA"));
    }

    // ── Listado, filtros y resumen ──────────────────────────────────

    @Test
    void filtros_porTextoNumeroEstadoVencidosYSaldo() throws Exception {
        String a =
                cuerpo(
                        crearPedido(
                                admin,
                                clienteNuevo("3001112233"),
                                prenda(tipo, 1, "10000"),
                                null,
                                null));
        String b =
                cuerpo(
                        crearPedido(
                                admin,
                                clienteNuevo("3004445566", "Pedro"),
                                prenda(tipo, 1, "10000"),
                                null,
                                abono("10000", "EFECTIVO")));
        String idA = JsonPath.read(a, "$.id");
        jdbc.update(
                "update pedidos set fecha_entrega = ? where id = ?",
                LocalDate.now().minusDays(2),
                UUID.fromString(idA));

        listar(admin, "buscar=pedro").andExpect(jsonPath("$.contenido", hasSize(1)));
        listar(admin, "buscar=3001112").andExpect(jsonPath("$.contenido[0].id").value(idA));
        // "#G-L" o "G-L" (sin el #, que en una URL habria que codificar).
        listar(admin, "buscar=" + JsonPath.read(b, "$.numero").toString().substring(1))
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].id").value((String) JsonPath.read(b, "$.id")));
        listar(admin, "vencidos=true")
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].vencido").value(true));
        listar(admin, "conSaldo=true")
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].id").value(idA));
        listar(admin, "estado=EN_PROCESO").andExpect(jsonPath("$.contenido", hasSize(2)));
        listar(admin, "estado=ENTREGADO").andExpect(jsonPath("$.contenido", hasSize(0)));
        listar(admin, "sinEntregar=true").andExpect(jsonPath("$.contenido", hasSize(2)));
        listar(admin, "sort=total,asc").andExpect(status().isBadRequest());

        mvc.perform(get("/api/v1/pedidos/resumen").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.enProceso").value(2))
                .andExpect(jsonPath("$.listosParaEntregar").value(0))
                .andExpect(jsonPath("$.vencidos").value(1))
                .andExpect(jsonPath("$.paraEntregarHoy").value(0));
    }

    // ── Clientes, ticket y aislamiento ──────────────────────────────

    @Test
    void unClienteConPedidosActivos_noSePuedeEliminar() throws Exception {
        String pedido =
                cuerpo(
                        crearPedido(
                                admin,
                                clienteNuevo("3001234567"),
                                prenda(tipo, 1, "10000"),
                                null,
                                null));
        String id = JsonPath.read(pedido, "$.id");
        String cliente = clienteId(pedido);
        String prenda = JsonPath.read(pedido, "$.prendas[0].id");

        eliminarCliente(cliente)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CLIENTE_CON_PEDIDOS_ACTIVOS"));
        estado(admin, id, prenda, "FINALIZADO");
        entregar(admin, id, List.of(prenda));
        // Entregado pero con saldo pendiente: sigue activo.
        eliminarCliente(cliente).andExpect(status().isUnprocessableEntity());
        abonar(admin, id, "10000", UUID.randomUUID().toString());
        eliminarCliente(cliente).andExpect(status().isNoContent());
    }

    @Test
    void ticket_traeDatosDeLaSastreriaYAbonosVigentes() throws Exception {
        String pedido =
                cuerpo(
                        crearPedido(
                                admin,
                                clienteNuevo("3001234567"),
                                prenda(tipo, 2, "15000"),
                                "5000",
                                abono("10000", "DAVIPLATA")));
        mvc.perform(
                        get("/api/v1/pedidos/{id}/ticket", (String) JsonPath.read(pedido, "$.id"))
                                .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sastreria.nombre").exists())
                .andExpect(jsonPath("$.prendas[0].tipoPrenda").value("Camisa"))
                .andExpect(jsonPath("$.total").value(25000))
                .andExpect(jsonPath("$.abonos[0].metodo").value("DAVIPLATA"))
                .andExpect(jsonPath("$.saldo").value(15000));
    }

    @Test
    void otraSastreria_noVeNiModificaMisPedidos() throws Exception {
        String pedido =
                cuerpo(
                        crearPedido(
                                admin,
                                clienteNuevo("3001234567"),
                                prenda(tipo, 1, "10000"),
                                null,
                                null));
        String id = JsonPath.read(pedido, "$.id");
        String otra =
                iniciarSesion(datos.crearDueno(datos.crearSastreria()).getEmail()).accessToken();

        mvc.perform(get("/api/v1/pedidos/{id}", id).header("Authorization", bearer(otra)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PEDIDO_NO_ENCONTRADO"));
        anular(otra, id, "Intento").andExpect(status().isNotFound());
        abonar(otra, id, "1000", UUID.randomUUID().toString()).andExpect(status().isNotFound());
        listar(otra, "").andExpect(jsonPath("$.totalElementos").value(0));
        // Ni usar un cliente ajeno.
        crearPedido(
                        otra,
                        clienteExistente(clienteId(pedido)),
                        prenda(crearTipo(otra, "Saco"), 1, "1000"),
                        null,
                        null)
                .andExpect(status().isNotFound());
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private String crearTipo(String token, String nombre) throws Exception {
        return JsonPath.read(
                cuerpo(
                        mvc.perform(
                                post("/api/v1/tipos-prenda")
                                        .header("Authorization", bearer(token))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"nombre\": \"" + nombre + "\"}"))),
                "$.id");
    }

    private static String clienteNuevo(String telefono) {
        return clienteNuevo(telefono, "Ana");
    }

    private static String clienteNuevo(String telefono, String nombre) {
        return """
                "clienteNuevo": {"nombre": "%s", "apellido": "Ruiz", "telefono": "%s"},"""
                .formatted(nombre, telefono);
    }

    private static String clienteExistente(String id) {
        return "\"clienteId\": \"" + id + "\",";
    }

    private static String prenda(String tipo, int cantidad, String precio) {
        return """
                {"tipoPrendaId": "%s", "cantidad": %d, "descripcion": "Ajuste de mangas", "precioUnitario": %s}"""
                .formatted(tipo, cantidad, precio);
    }

    private static String prendaConId(String id, String tipo, String precio) {
        return """
                {"id": "%s", "tipoPrendaId": "%s", "cantidad": 1, "descripcion": "Ajuste de mangas", "precioUnitario": %s}"""
                .formatted(id, tipo, precio);
    }

    private static String prendaConSastre(String tipo, String sastre) {
        return """
                {"tipoPrendaId": "%s", "sastreId": "%s", "cantidad": 1, "descripcion": "Ajuste de mangas", "precioUnitario": 1000}"""
                .formatted(tipo, sastre);
    }

    private static String abono(String monto, String metodo) {
        return "{\"monto\": " + monto + ", \"metodo\": \"" + metodo + "\"}";
    }

    private String cuerpoPedido(
            String cliente, String prendas, String descuento, String abonoInicial) {
        return cuerpoPedido(cliente, prendas, descuento, abonoInicial, manana);
    }

    private static String cuerpoPedido(
            String cliente, String prendas, String descuento, String abonoInicial, String entrega) {
        return "{"
                + cliente
                + "\"fechaEntrega\": \""
                + entrega
                + "\","
                + (descuento == null ? "" : "\"descuento\": " + descuento + ",")
                + (abonoInicial == null ? "" : "\"abonoInicial\": " + abonoInicial + ",")
                + "\"prendas\": ["
                + prendas
                + "]}";
    }

    private static String edicion(String entrega, String descuento, String prendas) {
        return cuerpoPedido("", prendas, descuento, null, entrega);
    }

    private ResultActions crearPedido(
            String token, String cliente, String prendas, String descuento, String abonoInicial)
            throws Exception {
        return enviarPedido(
                token,
                cuerpoPedido(cliente, prendas, descuento, abonoInicial),
                UUID.randomUUID().toString());
    }

    private ResultActions crearPedido(
            String token,
            String cliente,
            String prendas,
            String descuento,
            String abonoInicial,
            String entrega)
            throws Exception {
        return enviarPedido(
                token,
                cuerpoPedido(cliente, prendas, descuento, abonoInicial, entrega),
                UUID.randomUUID().toString());
    }

    private ResultActions enviarPedido(String token, String json, String clave) throws Exception {
        return mvc.perform(
                post("/api/v1/pedidos")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", clave)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json));
    }

    private ResultActions editar(String token, String id, String ifMatch, String json)
            throws Exception {
        var peticion =
                put("/api/v1/pedidos/{id}", id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json);
        if (ifMatch != null) {
            peticion.header("If-Match", ifMatch);
        }
        return mvc.perform(peticion);
    }

    private ResultActions estado(String token, String pedido, String prenda, String estado)
            throws Exception {
        return mvc.perform(
                patch("/api/v1/pedidos/{p}/prendas/{pr}/estado", pedido, prenda)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\": \"" + estado + "\"}"));
    }

    private ResultActions entregar(String token, String pedido, List<String> prendas)
            throws Exception {
        String ids = String.join("\",\"", prendas);
        return mvc.perform(
                patch("/api/v1/pedidos/{id}/entregar", pedido)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prendaIds\": [\"" + ids + "\"]}"));
    }

    private ResultActions anular(String token, String pedido, String razon) throws Exception {
        return mvc.perform(
                patch("/api/v1/pedidos/{id}/anular", pedido)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"razon\": \"" + razon + "\"}"));
    }

    private ResultActions abonar(String token, String pedido, String monto, String clave)
            throws Exception {
        return mvc.perform(
                post("/api/v1/pedidos/{id}/abonos", pedido)
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", clave)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(abono(monto, "EFECTIVO")));
    }

    private ResultActions listar(String token, String query) throws Exception {
        return mvc.perform(get("/api/v1/pedidos?" + query).header("Authorization", bearer(token)));
    }

    private ResultActions porTelefono(String token, String telefono) throws Exception {
        return mvc.perform(
                get("/api/v1/clientes/por-telefono/{t}", telefono)
                        .header("Authorization", bearer(token)));
    }

    private ResultActions eliminarCliente(String id) throws Exception {
        return mvc.perform(
                delete("/api/v1/clientes/{id}", id).header("Authorization", bearer(admin)));
    }

    private static String clienteId(String pedido) {
        return JsonPath.read(pedido, "$.cliente.id");
    }

    private static String cuerpo(ResultActions resultado) throws Exception {
        return resultado.andReturn().getResponse().getContentAsString();
    }
}
