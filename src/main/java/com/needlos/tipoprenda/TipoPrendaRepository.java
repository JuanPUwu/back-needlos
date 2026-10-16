package com.needlos.tipoprenda;

import com.needlos.tipoprenda.domain.TipoPrenda;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Catalogo de tipos de prenda. Las consultas JPQL ya filtran por sastreria (@TenantId). */
public interface TipoPrendaRepository extends JpaRepository<TipoPrenda, UUID> {

    List<TipoPrenda> findByActivoTrueOrderByNombreAsc();

    List<TipoPrenda> findAllByOrderByNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, UUID id);

    /**
     * Ultimo precio cobrado por un tipo de prenda en una sastreria, sin contar pedidos anulados.
     * Consulta nativa (cruza con pedidos sin depender de su modulo): el tenant se filtra a mano.
     */
    @Query(
            value =
                    """
                    select p.precio_unitario from prendas p
                    join pedidos o on o.id = p.pedido_id
                    where p.tenant_id = :tenantId and p.tipo_prenda_id = :tipoId
                      and p.eliminado = false and o.eliminado = false and o.anulado = false
                    order by p.creado_en desc
                    limit 1
                    """,
            nativeQuery = true)
    Optional<BigDecimal> ultimoPrecio(
            @Param("tenantId") UUID tenantId, @Param("tipoId") UUID tipoId);
}
