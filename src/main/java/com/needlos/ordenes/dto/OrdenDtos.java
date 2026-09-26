package com.needlos.ordenes.dto;

import com.needlos.ordenes.domain.EstadoPrenda;
import com.needlos.ordenes.domain.OrdenEstado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class OrdenDtos {

    private OrdenDtos() {
    }

    public record CrearOrdenRequest(
            @NotNull UUID clienteId,
            @NotNull @Future LocalDate fechaEntrega,
            @PositiveOrZero BigDecimal descuento,
            @NotEmpty @Valid List<PrendaRequest> prendas
    ) {
    }

    public record PrendaRequest(
            @NotNull UUID tipoPrendaId,
            UUID sastreId,
            @Min(1) int cantidad,
            @NotBlank @Size(min = 3, max = 200) String descripcion,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal precioUnitario
    ) {
    }

    public record CambiarEstadoRequest(
            @NotNull EstadoPrenda estado
    ) {
    }

    public record AnularOrdenRequest(
            @NotBlank @Size(min = 3, max = 300) String razon
    ) {
    }

    public record OrdenResponse(
            UUID id,
            long numero,
            UUID clienteId,
            LocalDate fecha,
            LocalDate fechaEntrega,
            OrdenEstado estado,
            BigDecimal subtotal,
            BigDecimal descuento,
            BigDecimal total,
            boolean anulada,
            String razonAnulacion,
            List<PrendaResponse> prendas
    ) {
    }

    public record PrendaResponse(
            UUID id,
            UUID tipoPrendaId,
            UUID sastreId,
            int cantidad,
            String descripcion,
            BigDecimal precioUnitario,
            BigDecimal subtotal,
            EstadoPrenda estado
    ) {
    }
}
