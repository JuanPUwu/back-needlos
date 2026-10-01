package com.needlos.security.cuenta.dto;

import com.needlos.common.validation.ContrasenaSegura;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTOs de la cuenta autenticada. */
public final class CuentaDtos {

    private CuentaDtos() {
    }

    public record CambiarContrasenaRequest(
            @NotBlank @Size(max = 72) String contrasenaActual,
            @ContrasenaSegura String contrasenaNueva
    ) {
    }
}
