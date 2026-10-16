package com.needlos.empleados;

/**
 * Un empleado nuevo (sin cuenta previa) fue habilitado: se le envia una invitacion con un enlace
 * para definir su contrasena. El correo se manda despues del commit.
 */
public record EmpleadoInvitado(
        String email, String nombre, String sastreria, String enlace, int horasValidez) {}
