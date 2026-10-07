package com.needlos.security.verificacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.needlos.common.correo.Correo;
import com.needlos.common.correo.EnviadorCorreo;
import com.needlos.security.acceso.AccesoRepository;
import com.needlos.security.cuenta.Cuenta;
import com.needlos.security.cuenta.CuentaRepository;
import com.needlos.security.google.GoogleTokenVerifier;
import com.needlos.security.google.GoogleTokenVerifier.UsuarioGoogle;
import com.needlos.soporte.IntegracionTest;
import com.needlos.tenant.TenantRepository;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Registro con correo y contrasena: cuenta sin verificar + codigo de 6 digitos (Manual F1,
 * respuesta a la pregunta del 2026-09-30). El registro con Google esta cubierto aparte
 * (auto-verificacion y bienvenida inmediata).
 */
class VerificacionCorreoIntegracionTest extends IntegracionTest {

    private static final Pattern CODIGO = Pattern.compile("\\b(\\d{6})\\b");
    private static final long ESPERA_MS = 5000;

    @MockitoBean private EnviadorCorreo enviador;

    @MockitoBean private GoogleTokenVerifier googleVerifier;

    @Autowired private CuentaRepository cuentaRepo;
    @Autowired private VerificacionCorreoRepository repo;
    @Autowired private AccesoRepository accesoRepo;
    @Autowired private TenantRepository tenantRepo;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void registroYVerificacion_entraDirectoYRecibeBienvenidaDespues() throws Exception {
        String email = "dueno-" + UUID.randomUUID() + "@prueba.com";
        registrar(email).andExpect(status().isCreated());

        Correo correoCodigo = esperarCorreo();
        assertThat(correoCodigo.asunto()).containsIgnoringCase("código");
        String codigo = extraerCodigo(correoCodigo);

        verificar(email, otroCodigo(codigo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CODIGO_VERIFICACION_INVALIDO"));

        MvcResult resultado =
                verificar(email, codigo)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.roles[0]").value("SASTRE_ADMIN"))
                        .andExpect(jsonPath("$.cuenta.email").value(email))
                        .andExpect(header().exists("Set-Cookie"))
                        .andReturn();
        assertThat(resultado.getResponse().getContentAsString()).isNotBlank();

        // La cuenta ya quedo verificada: ni el mismo codigo sirve de nuevo (no es una forma
        // alterna de iniciar sesion sin contrasena).
        verificar(email, codigo)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("CUENTA_YA_VERIFICADA"));

        Correo bienvenida = esperarSegundoCorreo();
        assertThat(bienvenida.asunto()).containsIgnoringCase("bienvenido");
        assertThat(cuentaRepo.findByEmailIgnoreCase(email).orElseThrow().isVerificada()).isTrue();

        login(email, "Clave123!").andExpect(status().isOk());
    }

    @Test
    void intentosAgotados_invalidanElCodigoYHayQuePedirUnoNuevo() throws Exception {
        String email = "dueno-" + UUID.randomUUID() + "@prueba.com";
        registrar(email).andExpect(status().isCreated());
        String codigoOriginal = extraerCodigo(esperarCorreo());

        for (int i = 0; i < 5; i++) {
            verificar(email, otroCodigo(codigoOriginal)).andExpect(status().isBadRequest());
        }
        // Agotados los intentos, ni el codigo correcto sirve ya: hay que pedir uno nuevo.
        verificar(email, codigoOriginal).andExpect(status().isBadRequest());

        reenviar(email).andExpect(status().isAccepted());
        String codigoNuevo = extraerCodigo(esperarSegundoCorreo());
        assertThat(codigoNuevo).isNotEqualTo(codigoOriginal);

        verificar(email, codigoNuevo).andExpect(status().isOk());
    }

    @Test
    void codigoVencido_esRechazado() throws Exception {
        String email = "dueno-" + UUID.randomUUID() + "@prueba.com";
        registrar(email).andExpect(status().isCreated());
        String codigo = extraerCodigo(esperarCorreo());

        Cuenta cuenta = cuentaRepo.findByEmailIgnoreCase(email).orElseThrow();
        VerificacionCorreo verificacion =
                repo.findFirstByCuentaIdOrderByCreadoEnDesc(cuenta.getId()).orElseThrow();
        verificacion.setExpira(Instant.now().minusSeconds(60));
        repo.save(verificacion);

        verificar(email, codigo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CODIGO_VERIFICACION_INVALIDO"));
    }

    @Test
    void reenviarCodigo_invalidaElAnteriorYSoloElNuevoSirve() throws Exception {
        String email = "dueno-" + UUID.randomUUID() + "@prueba.com";
        registrar(email).andExpect(status().isCreated());
        String codigoViejo = extraerCodigo(esperarCorreo());

        reenviar(email).andExpect(status().isAccepted());
        String codigoNuevo = extraerCodigo(esperarSegundoCorreo());

        verificar(email, codigoViejo).andExpect(status().isBadRequest());
        verificar(email, codigoNuevo).andExpect(status().isOk());
    }

    @Test
    void masDeTresReenviosPorHora_noEnviaMasCorreos() throws Exception {
        String email = "dueno-" + UUID.randomUUID() + "@prueba.com";
        registrar(email).andExpect(status().isCreated());
        esperarCorreo();

        for (int i = 0; i < 4; i++) {
            reenviar(email).andExpect(status().isAccepted());
        }

        // 1 del registro + 3 reenvios permitidos (el 4to se descarta sin avisar).
        verify(enviador, timeout(ESPERA_MS).times(4)).enviar(any());
    }

    @Test
    void reenviarCodigo_respondeIgualParaCorreoInexistenteOYaVerificada() throws Exception {
        reenviar("nadie-" + UUID.randomUUID() + "@prueba.com").andExpect(status().isAccepted());

        String emailGoogle = "dueno-google-" + UUID.randomUUID() + "@prueba.com";
        mockearGoogle(emailGoogle, "Ana");
        registrarGoogle().andExpect(status().isCreated());
        reenviar(emailGoogle).andExpect(status().isAccepted());

        // Ninguno de los dos casos debe generar un codigo nuevo.
        verify(enviador, timeout(1000).times(1)).enviar(any()); // solo la bienvenida de Google
    }

    @Test
    void registroConGoogle_entraDirectoSinCodigoYRecibeBienvenida() throws Exception {
        String email = "dueno-google-" + UUID.randomUUID() + "@prueba.com";
        mockearGoogle(email, "Marta");

        registrarGoogle()
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cuenta.email").value(email))
                .andExpect(header().exists("Set-Cookie"));

        assertThat(cuentaRepo.findByEmailIgnoreCase(email).orElseThrow().isVerificada()).isTrue();
        Correo bienvenida = esperarCorreo();
        assertThat(bienvenida.asunto()).containsIgnoringCase("bienvenido");
    }

    @Test
    void loginConGoogle_verificaUnaCuentaPendienteDeCodigoYEnviaBienvenida() throws Exception {
        String email = "dueno-" + UUID.randomUUID() + "@prueba.com";
        registrar(email).andExpect(status().isCreated());
        esperarCorreo(); // el codigo, que ya no hara falta

        mockearGoogle(email, "Ana");
        mvc.perform(
                        post("/api/v1/auth/google")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                        {"idToken": "token-de-prueba"}"""))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"));

        Cuenta cuenta = cuentaRepo.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(cuenta.isVerificada()).isTrue();
        // Anti "pre-secuestro": la contrasena puesta sin probar ser dueno del correo ya no sirve.
        assertThat(cuenta.getPasswordHash()).isNull();
        login(email, "Clave123!").andExpect(status().isUnauthorized());
        Correo bienvenida = esperarSegundoCorreo();
        assertThat(bienvenida.asunto()).containsIgnoringCase("bienvenido");
    }

    @Test
    void cuentaVerificadaConCodigo_despuesTambienEntraConGoogleConElMismoCorreo() throws Exception {
        String email = "dueno-" + UUID.randomUUID() + "@prueba.com";
        registrar(email).andExpect(status().isCreated());
        verificar(email, extraerCodigo(esperarCorreo())).andExpect(status().isOk());

        mockearGoogle(email, "Ana");
        mvc.perform(
                        post("/api/v1/auth/google")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                        {"idToken": "token-de-prueba"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sesion.cuenta.email").value(email));

        Cuenta cuenta = cuentaRepo.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(cuenta.getGoogleSub()).isNotNull();
        // La contrasena se conserva: esta cuenta SI probo ser dueña del correo con el codigo.
        assertThat(cuenta.getPasswordHash()).isNotNull();
        login(email, "Clave123!").andExpect(status().isOk());
    }

    // ── Registro repetido sobre un correo sin verificar ──────────────

    @Test
    void repetirElRegistro_reemplazaLaContrasenaYEnviaUnCodigoNuevo() throws Exception {
        String email = "dueno-" + UUID.randomUUID() + "@prueba.com";
        registrar(email).andExpect(status().isCreated());
        String codigoViejo = extraerCodigo(esperarCorreo());

        // Otra persona (o el mismo dueno) repite el registro con OTRA contrasena.
        registrarConContrasena(email, "Nueva123!", "Mi Sastreria")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.aviso").doesNotExist());
        String codigoNuevo = extraerCodigo(esperarSegundoCorreo());

        verificar(email, codigoViejo).andExpect(status().isBadRequest());
        verificar(email, codigoNuevo).andExpect(status().isOk());
        login(email, "Clave123!").andExpect(status().isUnauthorized());
        login(email, "Nueva123!").andExpect(status().isOk());
    }

    @Test
    void repetirConOtroNombreAntesDeLas24h_conservaLaSastreriaYAvisa() throws Exception {
        String email = "dueno-" + UUID.randomUUID() + "@prueba.com";
        registrarConContrasena(email, "Clave123!", "Sastreria Uno").andExpect(status().isCreated());

        registrarConContrasena(email, "Clave123!", "Sastreria Dos")
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.aviso").value(org.hamcrest.Matchers.containsString("24 horas")))
                .andExpect(
                        jsonPath("$.aviso")
                                .value(org.hamcrest.Matchers.containsString("Sastreria Uno")));

        assertThat(nombreDeSastreria(email)).isEqualTo("Sastreria Uno");
    }

    @Test
    void repetirConOtroNombreDespuesDeLas24h_cambiaLaSastreria() throws Exception {
        String email = "dueno-" + UUID.randomUUID() + "@prueba.com";
        registrarConContrasena(email, "Clave123!", "Sastreria Uno").andExpect(status().isCreated());
        jdbc.update(
                "update cuentas set creado_en = now() - interval '25 hours' where email = ?",
                email);

        registrarConContrasena(email, "Clave123!", "Sastreria Dos")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.aviso").doesNotExist());

        assertThat(nombreDeSastreria(email)).isEqualTo("Sastreria Dos");
    }

    @Test
    void registroConGoogleSobreUnCorreoPendiente_tomaLaCuentaYBorraLaContrasenaAjena()
            throws Exception {
        String email = "dueno-" + UUID.randomUUID() + "@prueba.com";
        registrar(email).andExpect(status().isCreated());
        esperarCorreo();

        mockearGoogle(email, "Marta");
        mvc.perform(
                        post("/api/v1/auth/registrar-sastreria-google")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"idToken": "token-de-prueba", "nombreSastreria": "La de Marta"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Set-Cookie"));

        Cuenta cuenta = cuentaRepo.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(cuenta.isVerificada()).isTrue();
        assertThat(cuenta.getPasswordHash()).isNull();
        assertThat(nombreDeSastreria(email)).isEqualTo("La de Marta");
        login(email, "Clave123!").andExpect(status().isUnauthorized());
    }

    private String nombreDeSastreria(String email) {
        Cuenta cuenta = cuentaRepo.findByEmailIgnoreCase(email).orElseThrow();
        var acceso = accesoRepo.findVigentes(cuenta.getId()).stream().findFirst().orElseThrow();
        return tenantRepo.findById(acceso.getTenantId()).orElseThrow().getNombre();
    }

    private ResultActions registrarConContrasena(String email, String contrasena, String sastreria)
            throws Exception {
        return mvc.perform(
                post("/api/v1/auth/registrar-sastreria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                        {"nombreSastreria": "%s", "nombreAdmin": "Ana", "apellidoAdmin": "Perez",
                         "numeroDocumento": "123", "email": "%s", "password": "%s"}"""
                                        .formatted(sastreria, email, contrasena)));
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private ResultActions registrar(String email) throws Exception {
        return mvc.perform(
                post("/api/v1/auth/registrar-sastreria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                        {"nombreSastreria": "Mi Sastreria", "nombreAdmin": "Ana", "apellidoAdmin": "Perez",
                         "numeroDocumento": "123", "email": "%s", "password": "Clave123!"}"""
                                        .formatted(email)));
    }

    private ResultActions registrarGoogle() throws Exception {
        return mvc.perform(
                post("/api/v1/auth/registrar-sastreria-google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                        {"idToken": "token-de-prueba", "nombreSastreria": "Mi Sastreria", "numeroDocumento": "123"}
                        """));
    }

    private ResultActions verificar(String email, String codigo) throws Exception {
        return mvc.perform(
                post("/api/v1/auth/verificar-correo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                        {"email": "%s", "codigo": "%s"}"""
                                        .formatted(email, codigo)));
    }

    private ResultActions reenviar(String email) throws Exception {
        return mvc.perform(
                post("/api/v1/auth/reenviar-codigo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                        {"email": "%s"}"""
                                        .formatted(email)));
    }

    private void mockearGoogle(String email, String nombre) {
        when(googleVerifier.verificar(anyString()))
                .thenReturn(
                        new UsuarioGoogle(UUID.randomUUID().toString(), email, nombre, "Prueba"));
    }

    private Correo esperarCorreo() {
        ArgumentCaptor<Correo> captor = ArgumentCaptor.forClass(Correo.class);
        verify(enviador, timeout(ESPERA_MS)).enviar(captor.capture());
        return captor.getValue();
    }

    /** Para cuando ya se espero un primer correo y hace falta esperar al segundo. */
    private Correo esperarSegundoCorreo() {
        ArgumentCaptor<Correo> captor = ArgumentCaptor.forClass(Correo.class);
        verify(enviador, timeout(ESPERA_MS).times(2)).enviar(captor.capture());
        return captor.getAllValues().get(1);
    }

    private static String extraerCodigo(Correo correo) {
        Matcher matcher = CODIGO.matcher(correo.texto());
        assertThat(matcher.find()).as("el correo trae el codigo de 6 digitos").isTrue();
        return matcher.group(1);
    }

    /** Siempre distinto de "distintoDe" (suma 1 modulo 1.000.000, nunca coincide). */
    private static String otroCodigo(String distintoDe) {
        int otro = (Integer.parseInt(distintoDe) + 1) % 1_000_000;
        return "%06d".formatted(otro);
    }
}
