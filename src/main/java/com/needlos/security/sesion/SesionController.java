package com.needlos.security.sesion;

import com.needlos.security.jwt.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Sesiones abiertas de la cuenta autenticada (sus dispositivos). */
@Tag(name = "Sesiones")
@SecurityRequirement(name = "Bearer")
@RestController
@RequestMapping("/api/v1/auth/sesiones")
public class SesionController {

    private final SesionService sesionService;

    public SesionController(SesionService sesionService) {
        this.sesionService = sesionService;
    }

    @Operation(summary = "Lista mis sesiones abiertas (marca la actual)")
    @GetMapping
    public List<SesionActivaResponse> listar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return sesionService.activas(usuario.cuentaId(), usuario.sesionId());
    }

    @Operation(summary = "Cierra una de mis sesiones (otro dispositivo o la actual)")
    @ApiResponse(responseCode = "204", description = "Sesion cerrada")
    @ApiResponse(responseCode = "404", description = "SESION_NO_ENCONTRADA")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cerrar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID id) {
        sesionService.cerrar(usuario.cuentaId(), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Cierra todas mis sesiones excepto la actual")
    @ApiResponse(responseCode = "204", description = "Sesiones cerradas")
    @DeleteMapping
    public ResponseEntity<Void> cerrarOtras(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        sesionService.cerrarOtras(usuario.cuentaId(), usuario.sesionId());
        return ResponseEntity.noContent().build();
    }
}
