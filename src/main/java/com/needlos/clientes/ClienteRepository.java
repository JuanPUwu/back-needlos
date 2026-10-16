package com.needlos.clientes;

import com.needlos.clientes.domain.Cliente;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * Si el cliente tiene pedidos activos: no anulados y con prendas sin entregar o saldo
     * pendiente. SQL nativo sobre las tablas de pedidos para no acoplar este modulo al de pedidos
     * (que ya depende de clientes).
     */
    @Query(
            value =
                    """
                    select exists (
                      select 1 from pedidos p
                      where p.cliente_id = :clienteId and p.eliminado = false and p.anulado = false
                        and (
                          exists (select 1 from prendas pr
                                  where pr.pedido_id = p.id and pr.eliminado = false
                                    and pr.estado <> 'ENTREGADO')
                          or coalesce((select sum(pr.precio_unitario * pr.cantidad) from prendas pr
                                       where pr.pedido_id = p.id and pr.eliminado = false), 0)
                             - p.descuento
                             > coalesce((select sum(a.monto) from abonos a
                                         where a.pedido_id = p.id and a.anulado = false), 0)
                        )
                    )
                    """,
            nativeQuery = true)
    boolean tienePedidosActivos(@Param("clienteId") UUID clienteId);

    /**
     * Saldo pendiente de cada cliente (suma, por pedido no anulado, de lo que falta por pagar).
     * Filas [cliente_id, saldo]; los clientes sin saldo no aparecen. Una consulta para toda la
     * pagina del listado.
     */
    @Query(
            value =
                    """
                    select t.cliente_id, sum(greatest(t.total - t.abonado, 0))
                    from (
                      select p.cliente_id,
                             coalesce((select sum(pr.precio_unitario * pr.cantidad) from prendas pr
                                       where pr.pedido_id = p.id and pr.eliminado = false), 0)
                               - p.descuento as total,
                             coalesce((select sum(a.monto) from abonos a
                                       where a.pedido_id = p.id and a.anulado = false), 0) as abonado
                      from pedidos p
                      where p.cliente_id in (:ids) and p.eliminado = false and p.anulado = false
                    ) t
                    group by t.cliente_id
                    having sum(greatest(t.total - t.abonado, 0)) > 0
                    """,
            nativeQuery = true)
    List<Object[]> saldosPendientes(@Param("ids") Collection<UUID> ids);
}
