package com.needlos.common.correo;

/**
 * Remitentes fijos de needlos.com (Manual F1, 2026-09-30). Cada tipo de correo usa el suyo para
 * que, con el tiempo, el usuario aprenda a reconocerlos de un vistazo y cualquier intento de
 * suplantacion se note por contraste. Las tres direcciones comparten el mismo dominio verificado en
 * Cloudflare, asi que no requieren configuracion adicional por separado.
 */
public enum Remitente {

    /** Recuperacion de contrasena, avisos de bloqueo y de cambio de contrasena. */
    SEGURIDAD("NeedlOS Seguridad <seguridad@needlos.com>", "pablys8@gmail.com"),

    /** Verificacion de correo al registrar una sastreria, bienvenida. */
    BIENVENIDA("NeedlOS <bienvenida@needlos.com>", null),

    /** Reservado: invitacion de empleado, licencia por vencer, etc. */
    NOTIFICACIONES("NeedlOS <notificaciones@needlos.com>", null);

    private final String direccion;
    private final String replyTo;

    Remitente(String direccion, String replyTo) {
        this.direccion = direccion;
        this.replyTo = replyTo;
    }

    public String direccion() {
        return direccion;
    }

    /** A donde llegan las respuestas del destinatario; null si no aplica. */
    public String replyTo() {
        return replyTo;
    }
}
