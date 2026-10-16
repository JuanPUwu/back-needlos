package com.needlos.empleados;

import com.needlos.empleados.dto.InvitacionDtos.AceptarInvitacionRequest;
import com.needlos.empleados.dto.InvitacionDtos.InvitacionInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Aceptacion de invitaciones. Publico: solo con el token del enlace del correo (no requiere
 * sesion).
 */
@Tag(name = "Invitaciones")
@RestController
@RequestMapping("/api/v1/invitaciones")
public class InvitacionController {

    private final InvitacionService service;

    public InvitacionController(InvitacionService service) {
        this.service = service;
    }

    @Operation(summary = "Datos de una invitación por su token (para la pantalla de aceptar)")
    @GetMapping("/{token}")
    public InvitacionInfo info(@PathVariable String token) {
        return service.info(token);
    }

    @Operation(
            summary = "Acepta la invitación",
            description =
                    "Crea la cuenta (si no existe, con la contraseña enviada) y el acceso a la "
                            + "sastrería. El enlace es de un solo uso.")
    @PostMapping("/{token}/aceptar")
    public ResponseEntity<Void> aceptar(
            @PathVariable String token, @Valid @RequestBody AceptarInvitacionRequest req) {
        service.aceptar(token, req.contrasena());
        return ResponseEntity.noContent().build();
    }
}
