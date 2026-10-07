package com.needlos.tipoprenda;

import com.needlos.tipoprenda.dto.TipoPrendaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Tipos de prenda")
@SecurityRequirement(name = "Bearer")
@RestController
@RequestMapping("/api/v1/tipos-prenda")
public class TipoPrendaController {

    private final TipoPrendaService service;

    public TipoPrendaController(TipoPrendaService service) {
        this.service = service;
    }

    @Operation(summary = "Lista los tipos de prenda activos del catálogo de la sastrería")
    @GetMapping
    public List<TipoPrendaResponse> listar() {
        return service.listarActivos();
    }
}
