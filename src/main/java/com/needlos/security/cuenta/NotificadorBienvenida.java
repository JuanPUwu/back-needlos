package com.needlos.security.cuenta;

import com.needlos.common.correo.Correo;
import com.needlos.common.correo.EnviadorCorreo;
import com.needlos.common.correo.PlantillaCorreo;
import com.needlos.common.correo.Remitente;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envia el correo de bienvenida cuando una cuenta queda verificada: al registrarse con Google
 * (verificado de inmediato) o, con correo y contrasena, al ingresar correctamente el codigo de
 * verificacion.
 */
@Component
public class NotificadorBienvenida {

    private static final String NOTA_PIE =
            "Si tú no creaste esta cuenta, escríbenos respondiendo este correo.";

    private final EnviadorCorreo enviador;
    private final String urlFrontend;

    public NotificadorBienvenida(
            EnviadorCorreo enviador,
            @Value("${needlos.recuperacion.url-frontend}") String urlFrontend) {
        this.enviador = enviador;
        this.urlFrontend = urlFrontend;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void enviar(CuentaVerificada evento) {
        String asunto = "¡Bienvenido a NeedlOS!";
        String texto =
                """
                Hola %s:

                Tu cuenta de NeedlOS ya esta lista. Gracias por registrarte.

                Inicia sesion aqui: %s/login

                NeedlOS
                """
                        .formatted(evento.nombre(), urlFrontend);

        List<String> parrafos =
                List.of(
                        "Hola " + evento.nombre() + ":",
                        "Tu cuenta de NeedlOS ya está lista. Gracias por registrarte.");
        String html =
                PlantillaCorreo.html(
                        "¡Bienvenido a NeedlOS!",
                        parrafos,
                        new PlantillaCorreo.Boton("Iniciar sesión", urlFrontend + "/login"),
                        NOTA_PIE);

        enviador.enviar(new Correo(evento.email(), Remitente.BIENVENIDA, asunto, texto, html));
    }
}
