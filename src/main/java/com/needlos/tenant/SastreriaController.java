package com.needlos.tenant;

import com.needlos.tenant.dto.SastreriaDtos.ActualizarSastreriaRequest;
import com.needlos.tenant.dto.SastreriaDtos.SastreriaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Configuracion de la sastreria propia. Solo el SASTRE_ADMIN. */
@Tag(name = "Sastrería")
@SecurityRequirement(name = "Bearer")
@RestController
@RequestMapping("/api/v1/sastreria")
@PreAuthorize("hasRole('SASTRE_ADMIN')")
public class SastreriaController {

    private final SastreriaService service;

    public SastreriaController(SastreriaService service) {
        this.service = service;
    }

    @Operation(summary = "Obtiene los datos de mi sastrería")
    @GetMapping
    public SastreriaResponse obtener() {
        return service.obtener();
    }

    @Operation(summary = "Actualiza los datos de mi sastrería (nombre y datos del ticket)")
    @PatchMapping
    public SastreriaResponse actualizar(@Valid @RequestBody ActualizarSastreriaRequest req) {
        return service.actualizar(req);
    }
}
