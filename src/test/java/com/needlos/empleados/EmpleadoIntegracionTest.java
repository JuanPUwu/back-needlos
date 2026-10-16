package com.needlos.empleados;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.needlos.common.correo.Correo;
import com.needlos.common.correo.EnviadorCorreo;
import com.needlos.security.cuenta.Cuenta;
import com.needlos.soporte.IntegracionTest;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

class EmpleadoIntegracionTest extends IntegracionTest {

    private static final Pattern TOKEN =
            Pattern.compile("/aceptar-invitacion#token=([A-Za-z0-9_-]+)");
    private static final long ESPERA_MS = 5000;

    @MockitoBean private EnviadorCorreo enviador;

    @Test
    void invitarNoAgregaAlInstante_creaInvitacionPendienteYEnviaCorreo() throws Exception {
        String admin = adminConSastreria();
        String email = "nuevo-" + UUID.randomUUID() + "@prueba.com";

        invitar(admin, email, "SASTRE").andExpect(status().isCreated());

        // No es miembro todavia; aparece como invitacion pendiente.
        assertThat(listar(admin)).doesNotContain(email);
        assertThat(listarInvitaciones(admin)).contains(email);
        assertThat(token(correoEnviado())).isNotBlank();
    }

    @Test
    void aceptarInvitacionNueva_creaCuentaConContrasenaYQuedaDeMiembro() throws Exception {
        String admin = adminConSastreria();
        String email = "ana-" + UUID.randomUUID() + "@prueba.com";
        invitar(admin, email, "SASTRE").andExpect(status().isCreated());
        String token = token(correoEnviado());

        mvc.perform(get("/api/v1/invitaciones/{t}", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.requiereContrasena").value(true));

        aceptar(token, "ClaveEmp1!").andExpect(status().isNoContent());

        assertThat(listar(admin)).contains(email);
        assertThat(listarInvitaciones(admin)).doesNotContain(email);
        login(email, "ClaveEmp1!").andExpect(status().isOk());
    }

    @Test
    void invitarCuentaExistente_aceptaSinContrasenaYSeAgregaElAcceso() throws Exception {
        // Una persona que ya trabaja en otra sastreria (ya tiene contrasena).
        Cuenta existente = datos.crearDueno(datos.crearSastreria());

        String admin = adminConSastreria();
        invitar(admin, existente.getEmail(), "SASTRE").andExpect(status().isCreated());
        String token = token(correoEnviado());

        mvc.perform(get("/api/v1/invitaciones/{t}", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiereContrasena").value(false));

        aceptar(token, null).andExpect(status().isNoContent());
        assertThat(listar(admin)).contains(existente.getEmail());
    }

    @Test
    void aceptarCuentaNuevaSinContrasena_esRechazado() throws Exception {
        String admin = adminConSastreria();
        String email = "sinpass-" + UUID.randomUUID() + "@prueba.com";
        invitar(admin, email, "SASTRE").andExpect(status().isCreated());
        String token = token(correoEnviado());

        aceptar(token, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"));
    }

    @Test
    void tokenInventado_esRechazado() throws Exception {
        mvc.perform(get("/api/v1/invitaciones/{t}", "no-existe"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVITACION_INVALIDA"));
    }

    @Test
    void elEnlaceNoSeReutiliza() throws Exception {
        String admin = adminConSastreria();
        String email = "unsolo-" + UUID.randomUUID() + "@prueba.com";
        invitar(admin, email, "SASTRE").andExpect(status().isCreated());
        String token = token(correoEnviado());

        aceptar(token, "ClaveEmp1!").andExpect(status().isNoContent());
        aceptar(token, "ClaveEmp1!")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVITACION_INVALIDA"));
    }

    @Test
    void invitarAAlguienQueYaEsDelEquipo_esRechazado() throws Exception {
        String admin = adminConSastreria();
        String email = "repetido-" + UUID.randomUUID() + "@prueba.com";
        invitar(admin, email, "SASTRE").andExpect(status().isCreated());
        aceptar(token(correoEnviado()), "ClaveEmp1!").andExpect(status().isNoContent());

        invitar(admin, email, "SASTRE")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMPLEADO_YA_EN_EQUIPO"));
    }

    @Test
    void cancelarInvitacionPendiente() throws Exception {
        String admin = adminConSastreria();
        String email = "cancelar-" + UUID.randomUUID() + "@prueba.com";
        String id =
                JsonPath.read(
                        invitar(admin, email, "SASTRE")
                                .andReturn()
                                .getResponse()
                                .getContentAsString(),
                        "$.id");

        mvc.perform(
                        delete("/api/v1/empleados/invitaciones/{id}", id)
                                .header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
        assertThat(listarInvitaciones(admin)).doesNotContain(email);
    }

    @Test
    void unSastreNoPuedeGestionarElEquipo() throws Exception {
        UUID tenant = datos.crearSastreria();
        datos.crearDueno(tenant);
        Cuenta sastre = datos.crearCuenta();
        datos.darAcceso(sastre, tenant, "SASTRE");
        String token = iniciarSesion(sastre.getEmail()).accessToken();

        mvc.perform(get("/api/v1/empleados").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void aislamiento_soloSeVenLasInvitacionesDeMiSastreria() throws Exception {
        String adminA = adminConSastreria();
        String emailA = "mio-" + UUID.randomUUID() + "@prueba.com";
        invitar(adminA, emailA, "SASTRE").andExpect(status().isCreated());

        String adminB = adminConSastreria();
        String emailB = "ajeno-" + UUID.randomUUID() + "@prueba.com";
        invitar(adminB, emailB, "SASTRE").andExpect(status().isCreated());

        assertThat(listarInvitaciones(adminA)).contains(emailA).doesNotContain(emailB);
    }

    @Test
    void noSePuedeDesactivarAlUltimoAdmin() throws Exception {
        UUID tenant = datos.crearSastreria();
        Cuenta dueno = datos.crearDueno(tenant);
        String admin = iniciarSesion(dueno.getEmail()).accessToken();
        String accesoId = accesoIdDe(admin, dueno.getEmail());

        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                                        "/api/v1/empleados/{id}/desactivar", accesoId)
                                .header("Authorization", bearer(admin)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ULTIMO_ADMIN"));
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private String adminConSastreria() throws Exception {
        Cuenta dueno = datos.crearDueno(datos.crearSastreria());
        return iniciarSesion(dueno.getEmail()).accessToken();
    }

    private ResultActions invitar(String token, String email, String rol) throws Exception {
        return mvc.perform(
                post("/api/v1/empleados")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"nombre":"Ana","apellido":"Pérez","email":"%s","telefono":"3001234567","numeroDocumento":"123","rol":"%s"}"""
                                        .formatted(email, rol)));
    }

    private ResultActions aceptar(String token, String contrasena) throws Exception {
        String cuerpo = contrasena == null ? "{}" : "{\"contrasena\":\"" + contrasena + "\"}";
        return mvc.perform(
                post("/api/v1/invitaciones/{t}/aceptar", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo));
    }

    private String listar(String token) throws Exception {
        return mvc.perform(get("/api/v1/empleados").header("Authorization", bearer(token)))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private String listarInvitaciones(String token) throws Exception {
        return mvc.perform(
                        get("/api/v1/empleados/invitaciones")
                                .header("Authorization", bearer(token)))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private String accesoIdDe(String token, String email) throws Exception {
        java.util.List<java.util.Map<String, Object>> equipo = JsonPath.read(listar(token), "$");
        return equipo.stream()
                .filter(e -> email.equalsIgnoreCase((String) e.get("email")))
                .map(e -> (String) e.get("accesoId"))
                .findFirst()
                .orElseThrow();
    }

    private Correo correoEnviado() {
        ArgumentCaptor<Correo> captor = ArgumentCaptor.forClass(Correo.class);
        Mockito.verify(enviador, Mockito.timeout(ESPERA_MS)).enviar(captor.capture());
        return captor.getValue();
    }

    private static String token(Correo correo) {
        Matcher matcher = TOKEN.matcher(correo.texto());
        assertThat(matcher.find()).as("la invitacion trae el enlace con el token").isTrue();
        return matcher.group(1);
    }
}
