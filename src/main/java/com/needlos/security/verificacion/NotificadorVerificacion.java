package com.needlos.security.verificacion;

import com.needlos.common.correo.Correo;
import com.needlos.common.correo.EnviadorCorreo;
import com.needlos.common.correo.PlantillaCorreo;
import com.needlos.common.correo.PlantillaCorreo.Codigo;
import com.needlos.common.correo.Remitente;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/** Envia el codigo de verificacion despues del commit y fuera del hilo de la peticion. */
@Component
public class NotificadorVerificacion {

    private final EnviadorCorreo enviador;

    public NotificadorVerificacion(EnviadorCorreo enviador) {
        this.enviador = enviador;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void enviar(VerificacionCorreoSolicitada evento) {
        String asunto = "Tu codigo para verificar tu cuenta de NeedlOS";
        String texto = """
                Hola %s:

                Para activar tu cuenta de NeedlOS, escribe este codigo en la pantalla de verificacion:

                %s

                Vence en %d minutos y solo sirve una vez.

                Si no fuiste tú, ignora este mensaje.

                NeedlOS
                """.formatted(evento.nombre(), evento.codigo(), evento.minutosValidez());

        List<String> parrafos = List.of(
                "Hola " + evento.nombre() + ":",
                "Para activar tu cuenta de NeedlOS, escribe este codigo en la pantalla de verificacion. "
                        + "Vence en " + evento.minutosValidez() + " minutos y solo sirve una vez.");
        String html = PlantillaCorreo.htmlConCodigo("Verifica tu correo", parrafos, new Codigo(evento.codigo()));

        enviador.enviar(new Correo(evento.email(), Remitente.BIENVENIDA, asunto, texto, html));
    }
}
