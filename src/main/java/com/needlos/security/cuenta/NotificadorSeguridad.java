package com.needlos.security.cuenta;

import com.needlos.common.correo.Correo;
import com.needlos.common.correo.EnviadorCorreo;
import com.needlos.common.correo.PlantillaCorreo;
import com.needlos.common.correo.Remitente;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * Avisos de seguridad al dueno de la cuenta (Manual F1, pregunta 47): si alguien
 * intenta entrar o cambia la contrasena, el dueno se entera. Responder el correo
 * llega a la bandeja del dueno de NeedlOS (reply-to del remitente de seguridad).
 */
@Component
public class NotificadorSeguridad {

    private static final String NOTA_PIE =
            "Si tú no hiciste esto, cambia tu contraseña cuanto antes o responde este correo.";

    private final EnviadorCorreo enviador;

    public NotificadorSeguridad(EnviadorCorreo enviador) {
        this.enviador = enviador;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void bloqueada(CuentaBloqueada evento) {
        String asunto = "Bloqueamos temporalmente el acceso a tu cuenta de NeedlOS";
        String detalle = "Detectamos varios intentos fallidos de iniciar sesión en tu cuenta, así que bloqueamos "
                + "el inicio de sesión con contraseña durante " + evento.minutos() + " minutos. "
                + "Si eras tú, espera ese tiempo o entra con Google si tu cuenta lo tiene vinculado. "
                + "Si no, puedes ignorar este aviso: tu contraseña sigue protegida.";
        enviar(evento.email(), asunto, "Bloqueo temporal de tu cuenta", evento.nombre(), detalle);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void contrasenaCambiada(ContrasenaCambiada evento) {
        String asunto = "Tu contraseña de NeedlOS cambió";
        String detalle = "La contraseña de tu cuenta se cambió hace un momento y, por seguridad, cerramos tus "
                + "otras sesiones abiertas.";
        enviar(evento.email(), asunto, "Tu contraseña cambió", evento.nombre(), detalle);
    }

    private void enviar(String para, String asunto, String titulo, String nombre, String detalle) {
        String texto = "Hola " + nombre + ":\n\n" + detalle + "\n\n" + NOTA_PIE + "\n\nNeedlOS\n";
        String html = PlantillaCorreo.html(titulo, List.of("Hola " + nombre + ":", detalle), null, NOTA_PIE);
        enviador.enviar(new Correo(para, Remitente.SEGURIDAD, asunto, texto, html));
    }
}
