package com.needlos.security.recuperacion;

import com.needlos.common.correo.Correo;
import com.needlos.common.correo.EnviadorCorreo;
import com.needlos.security.cuenta.Cuenta;
import com.needlos.security.token.TokenAleatorio;
import com.needlos.soporte.DatosPrueba;
import com.needlos.soporte.IntegracionTest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecuperacionContrasenaIntegracionTest extends IntegracionTest {

    private static final Pattern TOKEN = Pattern.compile("/restablecer-contrasena#token=([A-Za-z0-9_-]+)");
    private static final long ESPERA_MS = 5000;

    @MockitoBean
    private EnviadorCorreo enviador;

    @Autowired
    private RecuperacionContrasenaRepository repo;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void flujoCompleto_cambiaLaContrasenaCierraSesionesYElEnlaceNoSeReutiliza() throws Exception {
        Cuenta dueno = datos.crearDueno(datos.crearSastreria());
        SesionPrueba sesionAbierta = iniciarSesion(dueno.getEmail());

        solicitar(dueno.getEmail()).andExpect(status().isAccepted());
        Correo correo = correoEnviado();
        assertThat(correo.para()).isEqualTo(dueno.getEmail());
        assertThat(correo.texto()).contains("http://localhost:4200/restablecer-contrasena#token=");
        String token = token(correo);

        restablecer(token, "debil").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"));

        restablecer(token, "NuevaClave1!").andExpect(status().isNoContent());

        login(dueno.getEmail(), "NuevaClave1!").andExpect(status().isOk());
        login(dueno.getEmail(), DatosPrueba.CONTRASENA).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/sesiones").header("Authorization", bearer(sesionAbierta.accessToken())))
                .andExpect(status().isUnauthorized());
        refrescar(sesionAbierta.cookie()).andExpect(status().isUnauthorized());

        restablecer(token, "OtraClave1!").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ENLACE_RECUPERACION_INVALIDO"));
    }

    @Test
    void correoNoRegistrado_respondeIgualYNoEnviaNada() throws Exception {
        solicitar("nadie-" + UUID.randomUUID() + "@prueba.com").andExpect(status().isAccepted());

        verify(enviador, after(500).never()).enviar(any());
    }

    @Test
    void cuentaSinContrasena_recibeIndicacionDeEntrarConGoogleSinEnlace() throws Exception {
        Cuenta soloGoogle = datos.crearCuentaSinContrasena();

        solicitar(soloGoogle.getEmail()).andExpect(status().isAccepted());

        Correo correo = correoEnviado();
        assertThat(correo.texto()).contains("Continuar con Google").doesNotContain("token=");
    }

    @Test
    void enlaceVencido_esRechazado() throws Exception {
        Cuenta dueno = datos.crearDueno(datos.crearSastreria());
        solicitar(dueno.getEmail()).andExpect(status().isAccepted());
        String token = token(correoEnviado());

        // "expira" es updatable=false a proposito (nadie en la app debe poder extender un
        // enlace ya emitido): se fuerza el vencimiento con SQL directo, solo para la prueba.
        jdbc.update("update recuperaciones_contrasena set expira = ? where token_hash = ?",
                Timestamp.from(Instant.now().minusSeconds(60)), TokenAleatorio.hash(token));

        restablecer(token, "NuevaClave1!").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ENLACE_RECUPERACION_INVALIDO"));
    }

    @Test
    void soloElUltimoEnlaceSirve() throws Exception {
        Cuenta dueno = datos.crearDueno(datos.crearSastreria());
        solicitar(dueno.getEmail()).andExpect(status().isAccepted());
        solicitar(dueno.getEmail()).andExpect(status().isAccepted());

        ArgumentCaptor<Correo> correos = ArgumentCaptor.forClass(Correo.class);
        verify(enviador, timeout(ESPERA_MS).times(2)).enviar(correos.capture());
        String primero = token(correos.getAllValues().get(0));
        String segundo = token(correos.getAllValues().get(1));
        // Dos envios en segundo plano pueden llegar en cualquier orden: el nuevo es el que sigue en la BD.
        boolean primeroEsElNuevo = repo.findByTokenHash(TokenAleatorio.hash(primero)).isPresent();
        String viejo = primeroEsElNuevo ? segundo : primero;
        String nuevo = primeroEsElNuevo ? primero : segundo;

        restablecer(viejo, "NuevaClave1!").andExpect(status().isBadRequest());
        restablecer(nuevo, "NuevaClave1!").andExpect(status().isNoContent());
    }

    @Test
    void masDeTresSolicitudesPorHora_noEnvianMasCorreos() throws Exception {
        Cuenta dueno = datos.crearDueno(datos.crearSastreria());
        for (int i = 0; i < 4; i++) {
            solicitar(dueno.getEmail()).andExpect(status().isAccepted());
        }

        verify(enviador, after(1000).times(3)).enviar(any());
    }

    @Test
    void restablecer_levantaElBloqueoPorIntentosFallidos() throws Exception {
        Cuenta dueno = datos.crearDueno(datos.crearSastreria());
        for (int i = 0; i < 5; i++) {
            login(dueno.getEmail(), "Incorrecta1!");
        }
        login(dueno.getEmail(), DatosPrueba.CONTRASENA).andExpect(status().isTooManyRequests());
        correoEnviado(); // el aviso de bloqueo (no es el correo de recuperacion)

        solicitar(dueno.getEmail()).andExpect(status().isAccepted());
        ArgumentCaptor<Correo> correos = ArgumentCaptor.forClass(Correo.class);
        verify(enviador, timeout(ESPERA_MS).times(2)).enviar(correos.capture());
        Correo recuperacion = correos.getAllValues().stream()
                .filter(c -> c.asunto().contains("Restablece")).findFirst().orElseThrow();
        restablecer(token(recuperacion), "NuevaClave1!").andExpect(status().isNoContent());

        login(dueno.getEmail(), "NuevaClave1!").andExpect(status().isOk());
    }

    @Test
    void tokenInventado_esRechazado() throws Exception {
        restablecer("token-que-no-existe", "NuevaClave1!").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ENLACE_RECUPERACION_INVALIDO"));
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private ResultActions solicitar(String email) throws Exception {
        return mvc.perform(post("/api/v1/auth/recuperar-contrasena").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s"}""".formatted(email)));
    }

    private ResultActions restablecer(String token, String contrasena) throws Exception {
        return mvc.perform(post("/api/v1/auth/restablecer-contrasena").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"token": "%s", "contrasenaNueva": "%s"}""".formatted(token, contrasena)));
    }

    /** El correo se envia en segundo plano despues del commit: se espera a que llegue. */
    private Correo correoEnviado() {
        ArgumentCaptor<Correo> captor = ArgumentCaptor.forClass(Correo.class);
        verify(enviador, timeout(ESPERA_MS)).enviar(captor.capture());
        return captor.getValue();
    }

    private static String token(Correo correo) {
        Matcher matcher = TOKEN.matcher(correo.texto());
        assertThat(matcher.find()).as("el correo trae el enlace con el token").isTrue();
        return matcher.group(1);
    }
}
