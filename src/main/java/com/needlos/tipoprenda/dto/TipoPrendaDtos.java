package com.needlos.tipoprenda.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/** DTOs del catalogo de tipos de prenda. */
public final class TipoPrendaDtos {

    private TipoPrendaDtos() {}

    /** Al menos un caracter visible (no solo espacios). */
    private static final String CON_TEXTO = ".*\\S.*";

    /** Crear o editar: el precio base es opcional (solo sirve de sugerencia sin historico). */
    public record GuardarTipoPrendaRequest(
            @NotBlank @Size(max = 60) @Pattern(regexp = CON_TEXTO) String nombre,
            @DecimalMin(value = "0", message = "El precio base no puede ser negativo")
                    @Digits(integer = 10, fraction = 2)
                    BigDecimal precioBase) {}

    /** {@code version} se devuelve en If-Match al editar (bloqueo optimista). */
    public record TipoPrendaResponse(
            UUID id, String nombre, BigDecimal precioBase, boolean activo, long version) {}

    /** De donde sale el precio sugerido. */
    public enum OrigenPrecio {
        /** El ultimo precio cobrado por este tipo en la sastreria. */
        HISTORICO,
        /** No hay historico: el precio base del catalogo. */
        PRECIO_BASE,
        /** Ni historico ni precio base. */
        NINGUNO
    }

    public record PrecioSugeridoResponse(BigDecimal precio, OrigenPrecio origen) {}
}
