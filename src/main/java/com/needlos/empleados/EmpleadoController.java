package com.needlos.empleados;

import com.needlos.empleados.dto.EmpleadoDtos.ActualizarEmpleadoRequest;
import com.needlos.empleados.dto.EmpleadoDtos.CrearEmpleadoRequest;
import com.needlos.empleados.dto.EmpleadoDtos.EmpleadoResponse;
import com.needlos.empleados.dto.InvitacionDtos.InvitacionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Gestion del equipo de la sastreria. Solo el SASTRE_ADMIN de esa sastreria. */
@Tag(name = "Empleados")
@SecurityRequirement(name = "Bearer")
@RestController
@RequestMapping("/api/v1/empleados")
@PreAuthorize("hasRole('SASTRE_ADMIN')")
public class EmpleadoController {

    private final EmpleadoService service;

    public EmpleadoController(EmpleadoService service) {
        this.service = service;
    }

    @Operation(summary = "Lista los miembros del equipo (ya aceptados)")
    @GetMapping
    public List<EmpleadoResponse> listar() {
        return service.listar();
    }

    @Operation(
            summary = "Invita a un empleado por correo",
            description =
                    "No lo agrega todavía: crea una invitación pendiente y le envía un enlace para "
                            + "aceptarla. La cuenta y el acceso se crean cuando la persona acepta.")
    @PostMapping
    public ResponseEntity<InvitacionResponse> invitar(
            @Valid @RequestBody CrearEmpleadoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.invitar(req));
    }

    @Operation(summary = "Lista las invitaciones pendientes de aceptar")
    @GetMapping("/invitaciones")
    public List<InvitacionResponse> listarInvitaciones() {
        return service.listarInvitaciones();
    }

    @Operation(summary = "Reenvía una invitación pendiente (nuevo enlace)")
    @PostMapping("/invitaciones/{id}/reenviar")
    public ResponseEntity<Void> reenviarInvitacion(@PathVariable UUID id) {
        service.reenviarInvitacion(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Cancela una invitación pendiente")
    @DeleteMapping("/invitaciones/{id}")
    public ResponseEntity<Void> cancelarInvitacion(@PathVariable UUID id) {
        service.cancelarInvitacion(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Edita el rol y los datos de un miembro")
    @PutMapping("/{accesoId}")
    public EmpleadoResponse actualizar(
            @PathVariable UUID accesoId, @Valid @RequestBody ActualizarEmpleadoRequest req) {
        return service.actualizar(accesoId, req);
    }

    @Operation(summary = "Desactiva el acceso de un miembro (soft, conserva su historial)")
    @PatchMapping("/{accesoId}/desactivar")
    public ResponseEntity<Void> desactivar(@PathVariable UUID accesoId) {
        service.desactivar(accesoId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reactiva el acceso de un miembro")
    @PatchMapping("/{accesoId}/reactivar")
    public ResponseEntity<Void> reactivar(@PathVariable UUID accesoId) {
        service.reactivar(accesoId);
        return ResponseEntity.noContent().build();
    }
}
