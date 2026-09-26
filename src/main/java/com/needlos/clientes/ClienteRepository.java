package com.needlos.clientes;

import com.needlos.clientes.domain.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repositorio de clientes. No hace falta filtrar por tenant a mano: Hibernate lo
 * aplica automaticamente gracias a @TenantId en BaseEntity.
 */
public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    Page<Cliente> findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCase(
            String nombre, String apellido, Pageable pageable);
}
