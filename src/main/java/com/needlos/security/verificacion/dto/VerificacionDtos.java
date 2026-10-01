package com.needlos.security.verificacion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** DTOs de la verificacion de correo al registrarse con contrasena. */
public final class VerificacionDtos {

    private VerificacionDtos() {
    }

    public record VerificarCorreoRequest(
            @NotBlank @Email @Size(max = 120) String email,
            @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "El codigo debe tener 6 digitos") String codigo
    ) {
    }

    public record ReenviarCodigoRequest(
            @NotBlank @Email @Size(max = 120) String email
    ) {
    }
}
