package com.needlos.clientes;

import com.needlos.clientes.dto.ClienteDtos.ActualizarClienteRequest;
import com.needlos.clientes.dto.ClienteDtos.ClienteResponse;
import com.needlos.clientes.dto.ClienteDtos.CrearClienteRequest;
import com.needlos.common.web.IfMatch;
import com.needlos.common.web.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Clientes. Ver: todo el equipo. Crear y editar: permiso GESTIONAR_CLIENTES (el SASTRE_ADMIN lo
 * tiene siempre). Eliminar: solo SASTRE_ADMIN.
 */
@Tag(name = "Clientes")
@SecurityRequirement(name = "Bearer")
@RestController
@RequestMapping("/api/v1/clientes")
@PreAuthorize("hasAnyRole('SASTRE', 'SASTRE_ADMIN')")
public class ClienteController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @Operation(summary = "Crea un cliente")
    @PostMapping
    @PreAuthorize("@permisos.tiene('GESTIONAR_CLIENTES')")
    public ResponseEntity<ClienteResponse> crear(@Valid @RequestBody CrearClienteRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    @Operation(
            summary = "Lista clientes (paginado y filtrable)",
            description =
                    "Filtros opcionales: buscar (nombre, apellido o teléfono), desde y hasta"
                            + " (fecha de registro, yyyy-MM-dd). Ordenable por: nombre, apellido,"
                            + " fechaRegistro. Máximo 100 por página.")
    @GetMapping
    public PaginaResponse<ClienteResponse> listar(
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate hasta,
            Pageable pageable) {
        return PaginaResponse.de(service.listar(buscar, desde, hasta, pageable));
    }

    @Operation(
            summary = "Cliente dueño de un teléfono",
            description = "Para rellenar los datos al crear un pedido. 404 si nadie lo tiene.")
    @GetMapping("/por-telefono/{telefono}")
    public ClienteResponse porTelefono(@PathVariable String telefono) {
        return service.porTelefono(telefono);
    }

    @Operation(summary = "Obtiene un cliente por id")
    @GetMapping("/{id}")
    public ClienteResponse obtener(@PathVariable UUID id) {
        return service.obtener(id);
    }

    @Operation(
            summary = "Actualiza un cliente",
            description =
                    "Exige la cabecera If-Match con la versión leída; si otra persona lo modificó"
                            + " antes, responde 409 CONFLICTO_CONCURRENCIA.")
    @PutMapping("/{id}")
    @PreAuthorize("@permisos.tiene('GESTIONAR_CLIENTES')")
    public ClienteResponse actualizar(
            @PathVariable UUID id,
            @RequestHeader(value = IfMatch.CABECERA, required = false) String ifMatch,
            @Valid @RequestBody ActualizarClienteRequest req) {
        return service.actualizar(id, IfMatch.version(ifMatch), req);
    }

    @Operation(
            summary = "Elimina un cliente (borrado lógico). Solo SASTRE_ADMIN.",
            description = "No se puede si tiene pedidos activos (sin entregar o con saldo).")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SASTRE_ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
