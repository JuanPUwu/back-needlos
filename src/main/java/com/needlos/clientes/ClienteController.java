package com.needlos.clientes;

import com.needlos.clientes.dto.ClienteDtos.ActualizarClienteRequest;
import com.needlos.clientes.dto.ClienteDtos.ClienteResponse;
import com.needlos.clientes.dto.ClienteDtos.CrearClienteRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Clientes")
@SecurityRequirement(name = "Bearer")
@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @Operation(summary = "Crea un cliente")
    @PostMapping
    public ResponseEntity<ClienteResponse> crear(@Valid @RequestBody CrearClienteRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    @Operation(summary = "Lista clientes (paginado, con busqueda opcional)")
    @GetMapping
    public Page<ClienteResponse> listar(@RequestParam(required = false) String buscar, Pageable pageable) {
        return service.listar(buscar, pageable);
    }

    @Operation(summary = "Obtiene un cliente por id")
    @GetMapping("/{id}")
    public ClienteResponse obtener(@PathVariable UUID id) {
        return service.obtener(id);
    }

    @Operation(summary = "Actualiza un cliente")
    @PutMapping("/{id}")
    public ClienteResponse actualizar(@PathVariable UUID id,
                                      @Valid @RequestBody ActualizarClienteRequest req) {
        return service.actualizar(id, req);
    }

    @Operation(summary = "Elimina un cliente (borrado logico)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
