package com.needlos.clientes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public final class ClienteDtos {

    private ClienteDtos() {}

    private static final String TELEFONO = "^[0-9]{7,15}$";
    private static final String MSG_TELEFONO = "El teléfono debe tener entre 7 y 15 dígitos";

    public record CrearClienteRequest(
            @NotBlank @Size(min = 2, max = 60) String nombre,
            @NotBlank @Size(min = 2, max = 60) String apellido,
            @NotBlank @Pattern(regexp = TELEFONO, message = MSG_TELEFONO) String telefono) {}

    public record ActualizarClienteRequest(
            @NotBlank @Size(min = 2, max = 60) String nombre,
            @NotBlank @Size(min = 2, max = 60) String apellido,
            @NotBlank @Pattern(regexp = TELEFONO, message = MSG_TELEFONO) String telefono) {}

    /**
     * Cliente. {@code saldoPendiente}: lo que debe sumando sus pedidos no anulados. {@code version}
     * se devuelve en {@code If-Match} al editar (bloqueo optimista entre peticiones).
     */
    public record ClienteResponse(
            UUID id,
            String nombre,
            String apellido,
            String telefono,
            LocalDate fechaRegistro,
            BigDecimal saldoPendiente,
            long version) {}
}
