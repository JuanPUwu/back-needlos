package com.needlos.security.recuperacion;

import com.needlos.common.correo.Correo;
import com.needlos.common.correo.EnviadorCorreo;
import com.needlos.common.correo.PlantillaCorreo;
import com.needlos.common.correo.PlantillaCorreo.Boton;
import com.needlos.common.correo.Remitente;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/** Envia el correo de recuperacion despues del commit y fuera del hilo de la peticion. */
@Component
public class NotificadorRecuperacion {

    private final EnviadorCorreo enviador;

    public NotificadorRecuperacion(EnviadorCorreo enviador) {
        this.enviador = enviador;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void enviar(RecuperacionSolicitada evento) {
        enviador.enviar(evento.enlace() != null ? conEnlace(evento) : sinContrasena(evento));
    }

    private Correo conEnlace(RecuperacionSolicitada evento) {
        String asunto = "Restablece tu contraseña de NeedlOS";
        String texto = """
                Hola %s:

                Recibimos una solicitud para restablecer la contraseña de tu cuenta de NeedlOS.
                Para crear una nueva, abre este enlace (vence en %d minutos y solo sirve una vez):

                %s

                Si no fuiste tú, ignora este mensaje: tu contraseña actual sigue funcionando.

                NeedlOS
                """.formatted(evento.nombre(), evento.minutosValidez(), evento.enlace());

        List<String> parrafos = List.of(
                "Hola " + evento.nombre() + ":",
                "Recibimos una solicitud para restablecer la contraseña de tu cuenta de NeedlOS. "
                        + "Este enlace vence en " + evento.minutosValidez() + " minutos y solo sirve una vez.");
        String html = PlantillaCorreo.html("Restablece tu contraseña", parrafos,
                new Boton("Restablecer contraseña", evento.enlace()));

        return new Correo(evento.email(), Remitente.SEGURIDAD, asunto, texto, html);
    }

    private Correo sinContrasena(RecuperacionSolicitada evento) {
        String asunto = "Cómo entrar a tu cuenta de NeedlOS";
        String texto = """
                Hola %s:

                Recibimos una solicitud para restablecer la contraseña de tu cuenta de NeedlOS,
                pero tu cuenta no usa contraseña: entra con el botón "Continuar con Google"
                usando este mismo correo.

                Si no fuiste tú, ignora este mensaje.

                NeedlOS
                """.formatted(evento.nombre());

        List<String> parrafos = List.of(
                "Hola " + evento.nombre() + ":",
                "Recibimos una solicitud para restablecer la contraseña de tu cuenta de NeedlOS, "
                        + "pero tu cuenta no usa contraseña: entra con el botón \"Continuar con Google\" "
                        + "usando este mismo correo.");
        String html = PlantillaCorreo.html("Cómo entrar a tu cuenta", parrafos, null);

        return new Correo(evento.email(), Remitente.SEGURIDAD, asunto, texto, html);
    }
}
