package com.needlos.tipoprenda.domain;

import com.needlos.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SoftDelete;

/**
 * Tipo de prenda del catalogo de una sastreria (Camisa, Pantalon...). Es configurable por tenant:
 * cada sastreria define su propia lista con un precio base de referencia. El precio real de cada
 * prenda se fija en la orden.
 */
@Getter
@Setter
@Entity
@Table(name = "tipos_prenda")
@SoftDelete(columnName = "eliminado")
public class TipoPrenda extends BaseEntity {

    @Column(nullable = false)
    private String nombre;

    /** Precio de referencia. Puede ser null si la sastreria no fija uno por defecto. */
    @Column(name = "precio_base", precision = 12, scale = 2)
    private BigDecimal precioBase;

    @Column(nullable = false)
    private boolean activo = true;
}
