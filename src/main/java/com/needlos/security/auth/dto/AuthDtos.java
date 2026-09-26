package com.needlos.security.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** DTOs de autenticacion agrupados. */
public final class AuthDtos {

    private AuthDtos() {
    }

    /**
     * Alta de una nueva sastreria (tenant) junto con su usuario dueno
     * (SASTRE_ADMIN). Arranca en plan DEMO.
     */
    public record RegistrarSastreriaRequest(
            @NotBlank @Size(max = 120) String nombreSastreria,
            @NotBlank @Pattern(regexp = "^[a-z0-9-]{3,60}$",
                    message = "El slug solo admite minusculas, numeros y guiones") String slug,
            @NotBlank @Size(max = 60) String nombreAdmin,
            @NotBlank @Size(max = 60) String apellidoAdmin,
            @NotBlank @Size(max = 30) String numeroDocumento,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @Size(max = 20) String telefono
    ) {
    }

    public record LoginRequest(
            @NotBlank String slug,
            @NotBlank @Email String email,
            @NotBlank String password
    ) {
    }

    public record RefreshRequest(
            @NotBlank String refreshToken
    ) {
    }

    public record TokenResponse(
            String accessToken,
            String refreshToken,
            long expiraEnSegundos
    ) {
    }
}
