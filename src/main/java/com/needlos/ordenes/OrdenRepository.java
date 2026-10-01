package com.needlos.ordenes;

import com.needlos.ordenes.domain.Orden;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrdenRepository extends JpaRepository<Orden, UUID> {

    @EntityGraph(attributePaths = "prendas")
    Optional<Orden> findWithPrendasById(UUID id);

    /**
     * Pagina de ordenes. Las prendas NO se traen con fetch join (Hibernate
     * paginaria en memoria): se cargan por lotes (default_batch_fetch_size).
     */
    Page<Orden> findAllBy(Pageable pageable);
}
