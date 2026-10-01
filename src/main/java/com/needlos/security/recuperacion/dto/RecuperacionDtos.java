package com.needlos.security.recuperacion.dto;

import com.needlos.common.validation.ContrasenaSegura;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTOs de la recuperacion de contrasena. */
public final class RecuperacionDtos {

    private RecuperacionDtos() {
    }

    public record RecuperarContrasenaRequest(
            @NotBlank @Email @Size(max = 120) String email
    ) {
    }

    public record RestablecerContrasenaRequest(
            @NotBlank @Size(max = 100) String token,
            @ContrasenaSegura String contrasenaNueva
    ) {
    }
}
