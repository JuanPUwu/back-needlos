package com.needlos.tipoprenda.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TipoPrendaResponse(UUID id, String nombre, BigDecimal precioBase) {
}
