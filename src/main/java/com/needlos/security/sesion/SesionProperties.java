package com.needlos.security.sesion;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion de las sesiones (prefijo needlos.sesion).
 *
 * @param duracionDias dias sin uso tras los cuales la sesion vence (se renueva al rotar)
 * @param cookieSegura cookie del refresh solo por HTTPS (false unicamente en dev)
 * @param graciaRotacionSegundos ventana en la que el refresh recien rotado aun se acepta, para
 *     peticiones simultaneas de varias pestanas del mismo navegador
 */
@ConfigurationProperties(prefix = "needlos.sesion")
public record SesionProperties(
        long duracionDias, boolean cookieSegura, long graciaRotacionSegundos) {}
