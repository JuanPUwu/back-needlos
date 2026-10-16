package com.needlos.pedidos;

import com.needlos.pedidos.dto.PedidoDtos.AnularAbonoRequest;
import com.needlos.pedidos.dto.PedidoDtos.PedidoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Abonos (los de un pedido se registran en /pedidos/{id}/abonos). */
@Tag(name = "Pedidos")
@SecurityRequirement(name = "Bearer")
@RestController
@RequestMapping("/api/v1/abonos")
@PreAuthorize("hasRole('SASTRE_ADMIN')")
public class AbonoController {

    private final AbonoService service;

    public AbonoController(AbonoService service) {
        this.service = service;
    }

    @Operation(
            summary = "Anula un abono (solo SASTRE_ADMIN)",
            description =
                    "Motivo DEVOLUCION (se le regresó el dinero) o ERROR_REGISTRO, más una razón."
                            + " Devuelve el pedido actualizado.")
    @PatchMapping("/{id}/anular")
    public PedidoResponse anular(
            @PathVariable UUID id, @Valid @RequestBody AnularAbonoRequest req) {
        return service.anular(id, req);
    }
}
