package com.needlos.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

/** DTOs de la configuracion de la sastreria propia (datos del negocio). */
public final class SastreriaDtos {

    private SastreriaDtos() {}

    public record SastreriaResponse(
            UUID id,
            String nombre,
            String nit,
            String direccion,
            String telefono,
            String plan,
            Instant licenciaHasta,
            boolean activo) {}

    /** Solo el nombre es obligatorio; el resto son datos opcionales para el ticket. */
    public record ActualizarSastreriaRequest(
            @NotBlank @Size(max = 120) String nombre,
            @Size(max = 30) String nit,
            @Size(max = 200) String direccion,
            @Size(max = 20) String telefono) {}
}
