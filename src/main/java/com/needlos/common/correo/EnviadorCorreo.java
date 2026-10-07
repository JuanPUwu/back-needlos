package com.needlos.common.correo;

/**
 * Envia correos. Se llama siempre DESPUES del commit y fuera del hilo de la peticion (Reglas §10:
 * nada de IO externo dentro de una transaccion).
 */
public interface EnviadorCorreo {

    void enviar(Correo correo);
}
