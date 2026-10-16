package com.needlos.empleados.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** DTOs de la gestion de empleados (accesos de una sastreria). */
public final class EmpleadoDtos {

    private EmpleadoDtos() {}

    /** Roles que un SASTRE_ADMIN puede asignar a un empleado (SUPER_ADMIN no es asignable). */
    private static final String ROLES = "^(SASTRE|SASTRE_ADMIN)$";

    public record CrearEmpleadoRequest(
            @NotBlank @Size(max = 60) String nombre,
            @NotBlank @Size(max = 60) String apellido,
            @NotBlank @Email @Size(max = 120) String email,
            @Size(max = 20) String telefono,
            @Size(max = 30) String numeroDocumento,
            @NotBlank @Pattern(regexp = ROLES, message = "Rol no válido") String rol) {}

    public record ActualizarEmpleadoRequest(
            @NotBlank @Size(max = 60) String nombre,
            @NotBlank @Size(max = 60) String apellido,
            @Size(max = 20) String telefono,
            @Size(max = 30) String numeroDocumento,
            @NotBlank @Pattern(regexp = ROLES, message = "Rol no válido") String rol) {}

    /** Un miembro del equipo (ya acepto su invitacion). */
    public record EmpleadoResponse(
            UUID accesoId,
            UUID cuentaId,
            String nombre,
            String apellido,
            String email,
            String telefono,
            String numeroDocumento,
            List<String> roles,
            boolean activo) {}
}
