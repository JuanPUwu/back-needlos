package com.needlos.common.correo;

/**
 * Correo transaccional. Lleva texto plano y HTML (multipart): el texto plano
 * es el respaldo si el cliente no muestra HTML y ayuda a que el correo no se
 * marque como spam.
 */
public record Correo(String para, Remitente remitente, String asunto, String texto, String html) {
}
