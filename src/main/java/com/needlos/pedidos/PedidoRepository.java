package com.needlos.pedidos;

import com.needlos.pedidos.domain.Pedido;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Pedidos de la sastreria actual (Hibernate filtra por tenant y oculta los eliminados). En los
 * listados, prendas y abonos NO se traen con fetch join (Hibernate paginaria en memoria): se cargan
 * por lotes (default_batch_fetch_size). Los filtros se arman con {@link PedidoFiltros}.
 */
public interface PedidoRepository
        extends JpaRepository<Pedido, UUID>, JpaSpecificationExecutor<Pedido> {

    @EntityGraph(attributePaths = "prendas")
    Optional<Pedido> findWithPrendasById(UUID id);

    Optional<Pedido> findByClaveIdempotencia(UUID clave);

    /**
     * El pedido con bloqueo de fila hasta el fin de la transaccion: dos abonos simultaneos no
     * pueden superar juntos el saldo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = :id")
    Optional<Pedido> bloquear(@Param("id") UUID id);
}
