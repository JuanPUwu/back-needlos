package com.needlos.security.auth.dto;

import com.needlos.common.validation.ContrasenaSegura;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * DTOs de autenticacion. El refresh token NUNCA viaja en el cuerpo: solo en la
 * cookie HttpOnly que fija el servidor.
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    // ── Peticiones ──────────────────────────────────────────────────

    public record LoginRequest(
            @NotBlank @Email @Size(max = 120) String email,
            @NotBlank @Size(max = 72) String password
    ) {
    }

    /**
     * Alta de una sastreria nueva (plan DEMO) con su cuenta dueno (SASTRE_ADMIN).
     * Solo se pide el nombre: el identificador interno (slug) lo genera el sistema.
     */
    public record RegistrarSastreriaRequest(
            @NotBlank @Size(max = 120) String nombreSastreria,
            @NotBlank @Size(max = 60) String nombreAdmin,
            @NotBlank @Size(max = 60) String apellidoAdmin,
            @NotBlank @Size(max = 30) String numeroDocumento,
            @NotBlank @Email @Size(max = 120) String email,
            @ContrasenaSegura String password,
            @Size(max = 20) String telefono
    ) {
    }

    /** Elige la sastreria cuando la cuenta tiene acceso a varias. */
    public record SeleccionarSastreriaRequest(
            @NotBlank String preauthToken,
            @NotNull UUID tenantId
    ) {
    }

    /** Login con Google: el ID token que devuelve Google Identity Services. */
    public record GoogleLoginRequest(
            @NotBlank String idToken
    ) {
    }

    /** Registrar una sastreria usando la cuenta de Google (el dueno es SASTRE_ADMIN). */
    public record RegistrarSastreriaGoogleRequest(
            @NotBlank String idToken,
            @NotBlank @Size(max = 120) String nombreSastreria,
            @Size(max = 30) String numeroDocumento
    ) {
    }

    // ── Respuestas ──────────────────────────────────────────────────

    /** Resumen de una sastreria a la que la cuenta puede entrar. */
    public record SastreriaResumen(
            UUID tenantId,
            String nombre,
            String slug,
            List<String> roles
    ) {
    }

    /** Datos basicos de la cuenta autenticada (para mostrar en la interfaz). */
    public record CuentaResumen(
            UUID id,
            String email,
            String nombre,
            String apellido
    ) {
    }

    /**
     * Sesion iniciada: access token (solo en memoria del cliente), roles de la
     * sesion y la sastreria activa (null para SUPER_ADMIN).
     */
    public record SesionResponse(
            String accessToken,
            long expiraEnSegundos,
            List<String> roles,
            CuentaResumen cuenta,
            SastreriaResumen sastreria
    ) {
    }

    /**
     * Resultado del login:
     *  · requiereSeleccion=false -> viene la sesion.
     *  · requiereSeleccion=true  -> vienen preauthToken + lista de sastrerias.
     */
    public record LoginResponse(
            boolean requiereSeleccion,
            String preauthToken,
            List<SastreriaResumen> sastrerias,
            SesionResponse sesion
    ) {
    }

    /**
     * Registro con correo y contrasena: la cuenta y la sastreria ya existen,
     * pero falta confirmar el codigo que se envio a este correo para poder entrar.
     */
    public record RegistroPendienteResponse(
            String email
    ) {
    }
}
