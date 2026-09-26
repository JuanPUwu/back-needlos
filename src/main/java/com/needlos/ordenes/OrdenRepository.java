package com.needlos.ordenes;

import com.needlos.ordenes.domain.Orden;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface OrdenRepository extends JpaRepository<Orden, UUID> {

    @EntityGraph(attributePaths = "prendas")
    Optional<Orden> findWithPrendasById(UUID id);

    @EntityGraph(attributePaths = "prendas")
    Page<Orden> findAllBy(Pageable pageable);

    /**
     * Mayor consecutivo usado en el tenant actual. El filtro por tenant lo
     * aplica Hibernate automaticamente (@TenantId), asi que este maximo es
     * por sastreria.
     */
    @Query("select coalesce(max(o.numero), 0) from Orden o")
    long maxNumero();
}
