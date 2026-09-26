package com.needlos.clientes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public final class ClienteDtos {

    private ClienteDtos() {
    }

    public record CrearClienteRequest(
            @NotBlank @Size(min = 2, max = 60) String nombre,
            @NotBlank @Size(min = 2, max = 60) String apellido,
            @Pattern(regexp = "^[0-9]{7,15}$", message = "El telefono debe tener entre 7 y 15 digitos")
            String telefono
    ) {
    }

    public record ActualizarClienteRequest(
            @NotBlank @Size(min = 2, max = 60) String nombre,
            @NotBlank @Size(min = 2, max = 60) String apellido,
            @Pattern(regexp = "^[0-9]{7,15}$", message = "El telefono debe tener entre 7 y 15 digitos")
            String telefono
    ) {
    }

    public record ClienteResponse(
            UUID id,
            String nombre,
            String apellido,
            String telefono,
            LocalDate fechaRegistro
    ) {
    }
}
