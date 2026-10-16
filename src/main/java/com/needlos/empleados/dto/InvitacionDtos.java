package com.needlos.empleados.dto;

import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.UUID;

/** DTOs de las invitaciones de empleados. */
public final class InvitacionDtos {

    private InvitacionDtos() {}

    /** Misma politica que @ContrasenaSegura, pero opcional (solo cuentas nuevas la envian). */
    private static final String POLITICA =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,72}$";

    /** Una invitacion pendiente, para la lista del admin. */
    public record InvitacionResponse(
            UUID id,
            String email,
            String nombre,
            String apellido,
            String rol,
            Instant expira,
            Instant creadoEn) {}

    /** Datos que ve la persona al abrir el enlace de invitacion (endpoint publico). */
    public record InvitacionInfo(
            String sastreria,
            String email,
            String nombre,
            /* true si la persona debe definir una contrasena (cuenta nueva o sin forma de entrar). */
            boolean requiereContrasena) {}

    /** Aceptacion de la invitacion. La contrasena solo se envia cuando se requiere. */
    public record AceptarInvitacionRequest(
            @Pattern(
                            regexp = POLITICA,
                            message =
                                    "La contraseña debe tener entre 8 y 72 caracteres e incluir"
                                            + " mayúscula, minúscula, número y carácter especial.")
                    String contrasena) {}
}
