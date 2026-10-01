package com.needlos.security.sesion;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

/**
 * Datos del dispositivo que abre o renueva una sesion. Lo construye el
 * controlador (capa HTTP) para que los servicios no dependan de la peticion.
 */
public record ContextoCliente(String ip, String userAgent) {

    private static final int MAX_USER_AGENT = 255;

    public static ContextoCliente desde(HttpServletRequest request) {
        String ua = request.getHeader(HttpHeaders.USER_AGENT);
        if (ua != null && ua.length() > MAX_USER_AGENT) {
            ua = ua.substring(0, MAX_USER_AGENT);
        }
        return new ContextoCliente(request.getRemoteAddr(), ua);
    }

    /** Descripcion corta y legible del dispositivo, p. ej. "Chrome en Windows". */
    public String dispositivo() {
        if (userAgent == null || userAgent.isBlank()) {
            return "Dispositivo desconocido";
        }
        return navegador() + " en " + sistema();
    }

    private String navegador() {
        if (userAgent.contains("Edg/")) {
            return "Edge";
        }
        if (userAgent.contains("OPR/")) {
            return "Opera";
        }
        if (userAgent.contains("Firefox/")) {
            return "Firefox";
        }
        if (userAgent.contains("Chrome/")) {
            return "Chrome";
        }
        if (userAgent.contains("Safari/")) {
            return "Safari";
        }
        return "Navegador";
    }

    private String sistema() {
        if (userAgent.contains("Windows")) {
            return "Windows";
        }
        if (userAgent.contains("Android")) {
            return "Android";
        }
        if (userAgent.contains("iPhone") || userAgent.contains("iPad")) {
            return "iOS";
        }
        if (userAgent.contains("Mac OS")) {
            return "macOS";
        }
        if (userAgent.contains("Linux")) {
            return "Linux";
        }
        return "otro sistema";
    }
}
