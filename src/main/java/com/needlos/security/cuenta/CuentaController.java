package com.needlos.security.cuenta;

import com.needlos.security.cuenta.dto.CuentaDtos.CambiarContrasenaRequest;
import com.needlos.security.jwt.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Operaciones de la cuenta autenticada sobre si misma. */
@Tag(name = "Mi cuenta")
@SecurityRequirement(name = "Bearer")
@RestController
@RequestMapping("/api/v1/cuenta")
public class CuentaController {

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @Operation(summary = "Cambia mi contraseña y cierra mis otras sesiones")
    @ApiResponse(responseCode = "204", description = "Contraseña cambiada")
    @ApiResponse(responseCode = "400", description = "VALIDACION (política de contraseñas)")
    @ApiResponse(
            responseCode = "422",
            description = "CONTRASENA_ACTUAL_INCORRECTA o CUENTA_SIN_CONTRASENA")
    @PatchMapping("/contrasena")
    public ResponseEntity<Void> cambiarContrasena(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody CambiarContrasenaRequest req) {
        cuentaService.cambiarContrasena(usuario.cuentaId(), usuario.sesionId(), req);
        return ResponseEntity.noContent().build();
    }
}
