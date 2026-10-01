package com.needlos.ordenes;

import com.needlos.ordenes.dto.OrdenDtos.AnularOrdenRequest;
import com.needlos.ordenes.dto.OrdenDtos.CambiarEstadoRequest;
import com.needlos.ordenes.dto.OrdenDtos.CrearOrdenRequest;
import com.needlos.ordenes.dto.OrdenDtos.OrdenResponse;
import com.needlos.common.web.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Ordenes")
@SecurityRequirement(name = "Bearer")
@RestController
@RequestMapping("/api/v1/ordenes")
public class OrdenController {

    private final OrdenService service;

    public OrdenController(OrdenService service) {
        this.service = service;
    }

    @Operation(summary = "Crea una orden con sus prendas")
    @PostMapping
    public ResponseEntity<OrdenResponse> crear(@Valid @RequestBody CrearOrdenRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    @Operation(summary = "Lista ordenes (paginado)",
            description = "Ordenable por: numero, fecha, fechaEntrega. Maximo 100 por pagina.")
    @GetMapping
    public PaginaResponse<OrdenResponse> listar(Pageable pageable) {
        return PaginaResponse.de(service.listar(pageable));
    }

    @Operation(summary = "Obtiene una orden por id")
    @GetMapping("/{id}")
    public OrdenResponse obtener(@PathVariable UUID id) {
        return service.obtener(id);
    }

    @Operation(summary = "Cambia el estado de una prenda de la orden")
    @PatchMapping("/{ordenId}/prendas/{prendaId}/estado")
    public OrdenResponse cambiarEstado(@PathVariable UUID ordenId,
                                       @PathVariable UUID prendaId,
                                       @Valid @RequestBody CambiarEstadoRequest req) {
        return service.cambiarEstadoPrenda(ordenId, prendaId, req.estado());
    }

    @Operation(summary = "Anula una orden (solo SASTRE_ADMIN, requiere razon)")
    @PreAuthorize("hasRole('SASTRE_ADMIN')")
    @PatchMapping("/{id}/anular")
    public OrdenResponse anular(@PathVariable UUID id, @Valid @RequestBody AnularOrdenRequest req) {
        return service.anular(id, req.razon());
    }
}
