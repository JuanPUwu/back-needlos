package com.needlos.tipoprenda;

import com.needlos.common.web.IfMatch;
import com.needlos.tipoprenda.dto.TipoPrendaDtos.GuardarTipoPrendaRequest;
import com.needlos.tipoprenda.dto.TipoPrendaDtos.PrecioSugeridoResponse;
import com.needlos.tipoprenda.dto.TipoPrendaDtos.TipoPrendaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Catalogo de tipos de prenda. Listar: todo el equipo. Gestionar: solo SASTRE_ADMIN. */
@Tag(name = "Tipos de prenda")
@SecurityRequirement(name = "Bearer")
@RestController
@RequestMapping("/api/v1/tipos-prenda")
@PreAuthorize("hasAnyRole('SASTRE', 'SASTRE_ADMIN')")
public class TipoPrendaController {

    private static final String SOLO_ADMIN = "hasRole('SASTRE_ADMIN')";

    private final TipoPrendaService service;

    public TipoPrendaController(TipoPrendaService service) {
        this.service = service;
    }

    @Operation(
            summary = "Lista el catálogo de la sastrería",
            description = "Por defecto solo activos; todos=true incluye los desactivados (admin).")
    @GetMapping
    public List<TipoPrendaResponse> listar(@RequestParam(defaultValue = "false") boolean todos) {
        return service.listar(todos);
    }

    @Operation(summary = "Precio sugerido: el último cobrado o, si no hay, el precio base")
    @GetMapping("/{id}/precio-sugerido")
    public PrecioSugeridoResponse precioSugerido(@PathVariable UUID id) {
        return service.precioSugerido(id);
    }

    @Operation(summary = "Crea un tipo de prenda (nombre único en la sastrería)")
    @PostMapping
    @PreAuthorize(SOLO_ADMIN)
    public ResponseEntity<TipoPrendaResponse> crear(
            @Valid @RequestBody GuardarTipoPrendaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    @Operation(summary = "Renombra o cambia el precio base (exige If-Match con la versión)")
    @PutMapping("/{id}")
    @PreAuthorize(SOLO_ADMIN)
    public TipoPrendaResponse actualizar(
            @PathVariable UUID id,
            @RequestHeader(value = IfMatch.CABECERA, required = false) String ifMatch,
            @Valid @RequestBody GuardarTipoPrendaRequest req) {
        return service.actualizar(id, IfMatch.version(ifMatch), req);
    }

    @Operation(summary = "Desactiva un tipo (no se ofrece en pedidos nuevos; no se borra)")
    @PatchMapping("/{id}/desactivar")
    @PreAuthorize(SOLO_ADMIN)
    public TipoPrendaResponse desactivar(@PathVariable UUID id) {
        return service.desactivar(id);
    }

    @Operation(summary = "Reactiva un tipo de prenda")
    @PatchMapping("/{id}/reactivar")
    @PreAuthorize(SOLO_ADMIN)
    public TipoPrendaResponse reactivar(@PathVariable UUID id) {
        return service.reactivar(id);
    }
}
