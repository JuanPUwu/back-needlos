package com.needlos.tipoprenda;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Tag(name = "Tipos de prenda")
@SecurityRequirement(name = "Bearer")
@RestController
@RequestMapping("/api/tipos-prenda")
public class TipoPrendaController {

    private final TipoPrendaRepository repo;

    public TipoPrendaController(TipoPrendaRepository repo) {
        this.repo = repo;
    }

    @Operation(summary = "Lista los tipos de prenda activos del catalogo del tenant")
    @GetMapping
    public List<TipoPrendaResponse> listar() {
        return repo.findByActivoTrueOrderByNombreAsc().stream()
                .map(t -> new TipoPrendaResponse(t.getId(), t.getNombre(), t.getPrecioBase()))
                .toList();
    }

    public record TipoPrendaResponse(UUID id, String nombre, BigDecimal precioBase) {
    }
}
