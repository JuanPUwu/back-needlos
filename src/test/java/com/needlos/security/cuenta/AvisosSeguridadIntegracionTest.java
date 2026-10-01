package com.needlos.security.cuenta;

import com.needlos.common.correo.Correo;
import com.needlos.common.correo.EnviadorCorreo;
import com.needlos.common.correo.Remitente;
import com.needlos.soporte.DatosPrueba;
import com.needlos.soporte.IntegracionTest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Avisos de seguridad por correo (Manual F1, pregunta 47). */
class AvisosSeguridadIntegracionTest extends IntegracionTest {

    private static final long ESPERA_MS = 5000;

    @MockitoBean
    private EnviadorCorreo enviador;

    @Test
    void alBloquearseElLogin_avisaAlDuenoUnaSolaVez() throws Exception {
        Cuenta dueno = datos.crearDueno(datos.crearSastreria());
        for (int i = 0; i < 6; i++) {
            login(dueno.getEmail(), "Incorrecta1!");
        }

        ArgumentCaptor<Correo> captor = ArgumentCaptor.forClass(Correo.class);
        verify(enviador, timeout(ESPERA_MS)).enviar(captor.capture());
        Correo aviso = captor.getValue();
        assertThat(aviso.para()).isEqualTo(dueno.getEmail());
        assertThat(aviso.remitente()).isEqualTo(Remitente.SEGURIDAD);
        assertThat(aviso.asunto()).containsIgnoringCase("bloque");
        // Los intentos durante el bloqueo no vuelven a avisar.
        verify(enviador, after(500).times(1)).enviar(any());
    }

    @Test
    void correoInexistente_alBloquearse_noEnviaNada() throws Exception {
        for (int i = 0; i < 6; i++) {
            login("nadie-" + java.util.UUID.randomUUID() + "@prueba.com", "Incorrecta1!");
        }

        verify(enviador, after(700).never()).enviar(any());
    }

    @Test
    void alCambiarLaContrasena_avisaAlDueno() throws Exception {
        Cuenta dueno = datos.crearDueno(datos.crearSastreria());
        SesionPrueba sesion = iniciarSesion(dueno.getEmail());

        mvc.perform(patch("/api/v1/cuenta/contrasena").header("Authorization", bearer(sesion.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contrasenaActual": "%s", "contrasenaNueva": "NuevaClave1!"}"""
                                .formatted(DatosPrueba.CONTRASENA)))
                .andExpect(status().isNoContent());

        ArgumentCaptor<Correo> captor = ArgumentCaptor.forClass(Correo.class);
        verify(enviador, timeout(ESPERA_MS)).enviar(captor.capture());
        assertThat(captor.getValue().para()).isEqualTo(dueno.getEmail());
        assertThat(captor.getValue().asunto()).containsIgnoringCase("cambi");
    }
}
