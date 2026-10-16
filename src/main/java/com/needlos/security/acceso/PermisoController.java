package com.needlos.security.acceso;

import com.needlos.security.acceso.PermisoService.MisPermisosResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Permisos")
@SecurityRequirement(name = "Bearer")
@RestController
@RequestMapping("/api/v1/permisos")
@PreAuthorize("hasAnyRole('SASTRE', 'SASTRE_ADMIN')")
public class PermisoController {

    private final PermisoService service;

    public PermisoController(PermisoService service) {
        this.service = service;
    }

    @Operation(
            summary = "Mis permisos en la sastrería activa",
            description =
                    "El SASTRE_ADMIN los tiene todos. Incluye mi accesoId (para el filtro"
                            + " Mis prendas).")
    @GetMapping("/mios")
    public MisPermisosResponse mios() {
        return service.mios();
    }
}
