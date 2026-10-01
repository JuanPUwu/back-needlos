package com.needlos.security.recuperacion;

import com.needlos.security.recuperacion.dto.RecuperacionDtos.RecuperarContrasenaRequest;
import com.needlos.security.recuperacion.dto.RecuperacionDtos.RestablecerContrasenaRequest;
import com.needlos.security.sesion.ContextoCliente;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints publicos de recuperacion de contrasena por correo. */
@Tag(name = "Autenticacion")
@RestController
@RequestMapping("/api/v1/auth")
public class RecuperacionContrasenaController {

    private final RecuperacionContrasenaService service;

    public RecuperacionContrasenaController(RecuperacionContrasenaService service) {
        this.service = service;
    }

    @Operation(summary = "Solicita un enlace para restablecer la contrasena",
            description = "Responde siempre 202, exista o no el correo (no revela cuentas registradas).")
    @ApiResponse(responseCode = "202", description = "Solicitud recibida")
    @ApiResponse(responseCode = "429", description = "DEMASIADAS_SOLICITUDES")
    @PostMapping("/recuperar-contrasena")
    public ResponseEntity<Void> solicitar(@Valid @RequestBody RecuperarContrasenaRequest req,
                                          HttpServletRequest request) {
        service.solicitar(req.email(), ContextoCliente.desde(request));
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Restablece la contrasena con el token del enlace y cierra todas las sesiones")
    @ApiResponse(responseCode = "204", description = "Contrasena cambiada")
    @ApiResponse(responseCode = "400", description = "VALIDACION o ENLACE_RECUPERACION_INVALIDO")
    @PostMapping("/restablecer-contrasena")
    public ResponseEntity<Void> restablecer(@Valid @RequestBody RestablecerContrasenaRequest req) {
        service.restablecer(req.token(), req.contrasenaNueva());
        return ResponseEntity.noContent().build();
    }
}
