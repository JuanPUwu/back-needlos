package com.needlos.clientes;

import com.needlos.clientes.domain.Cliente;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Repositorio de clientes. No hace falta filtrar por tenant a mano: Hibernate lo aplica
 * automaticamente gracias a @TenantId en BaseEntity (y @SoftDelete oculta los eliminados). Los
 * filtros del listado se arman con {@link ClienteFiltros}.
 */
public interface ClienteRepository
        extends JpaRepository<Cliente, UUID>, JpaSpecificationExecutor<Cliente> {

    Optional<Cliente> findByTelefono(String telefono);

    boolean existsByTelefono(String telefono);

    boolean existsByTelefonoAndIdNot(String telefono, UUID id);
}
