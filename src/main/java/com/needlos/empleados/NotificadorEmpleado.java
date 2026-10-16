package com.needlos.empleados;

import com.needlos.common.correo.Correo;
import com.needlos.common.correo.EnviadorCorreo;
import com.needlos.common.correo.PlantillaCorreo;
import com.needlos.common.correo.PlantillaCorreo.Boton;
import com.needlos.common.correo.Remitente;
import java.util.List;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Envia el correo de invitacion despues del commit y fuera del hilo de la peticion. */
@Component
public class NotificadorEmpleado {

    private final EnviadorCorreo enviador;

    public NotificadorEmpleado(EnviadorCorreo enviador) {
        this.enviador = enviador;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void invitar(EmpleadoInvitado evento) {
        // La sastreria va en el titulo (en negrita) para que se distinga del resto del texto.
        String titulo = "Te invitaron a " + evento.sastreria();
        String asunto = titulo + " en NeedlOS";
        String texto =
                """
                Hola %s:

                Te invitaron a unirte al equipo de %s en NeedlOS. Para aceptar la invitación,
                abre este enlace (vence en %d horas y solo sirve una vez):

                %s

                Si es tu primera vez, al aceptar defines tu contraseña; si ya tienes una cuenta,
                entras con la de siempre (contraseña o Google).

                NeedlOS
                """
                        .formatted(
                                evento.nombre(),
                                evento.sastreria(),
                                evento.horasValidez(),
                                evento.enlace());

        List<String> parrafos =
                List.of(
                        "Hola " + evento.nombre() + ":",
                        "Te invitaron a unirte a este equipo en NeedlOS. Para aceptar, pulsa el "
                                + "botón. Este enlace vence en "
                                + evento.horasValidez()
                                + " horas y solo sirve una vez.",
                        "Si es tu primera vez, al aceptar defines tu contraseña; si ya tienes una "
                                + "cuenta, entras con la de siempre (contraseña o Google).");
        String html =
                PlantillaCorreo.html(
                        titulo, parrafos, new Boton("Aceptar invitación", evento.enlace()));

        enviador.enviar(new Correo(evento.email(), Remitente.NOTIFICACIONES, asunto, texto, html));
    }
}
