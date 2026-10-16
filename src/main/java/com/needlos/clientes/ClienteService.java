package com.needlos.clientes;

import com.needlos.clientes.domain.Cliente;
import com.needlos.clientes.dto.ClienteDtos.ActualizarClienteRequest;
import com.needlos.clientes.dto.ClienteDtos.ClienteResponse;
import com.needlos.clientes.dto.ClienteDtos.CrearClienteRequest;
import com.needlos.common.exception.CodigoError;
import com.needlos.common.exception.ConflictoException;
import com.needlos.common.exception.RecursoNoEncontradoException;
import com.needlos.common.exception.ReglaNegocioException;
import com.needlos.common.exception.SolicitudInvalidaException;
import com.needlos.common.web.IfMatch;
import com.needlos.common.web.Ordenamiento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Clientes de la sastreria. El telefono es obligatorio y UNICO por sastreria (es el dato con el que
 * se identifica al cliente al crear un pedido, ver {@link #porTelefono}); se puede cambiar desde la
 * edicion. Eliminar es borrado logico y libera el numero.
 */
@Service
@Transactional
public class ClienteService {

    private static final Set<String> ORDENABLES = Set.of("nombre", "apellido", "fechaRegistro");

    private final ClienteRepository repo;

    public ClienteService(ClienteRepository repo) {
        this.repo = repo;
    }

    public ClienteResponse crear(CrearClienteRequest req) {
        String telefono = req.telefono().trim();
        if (repo.existsByTelefono(telefono)) {
            throw telefonoEnUso(telefono);
        }
        Cliente c = new Cliente();
        c.setNombre(req.nombre().trim());
        c.setApellido(req.apellido().trim());
        c.setTelefono(telefono);
        return toResponse(repo.save(c), BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public Page<ClienteResponse> listar(
            String buscar, LocalDate desde, LocalDate hasta, Pageable pageable) {
        Ordenamiento.validar(pageable, ORDENABLES);
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new SolicitudInvalidaException(
                    CodigoError.SOLICITUD_INVALIDA,
                    "La fecha inicial no puede ser posterior a la final.");
        }
        Page<Cliente> pagina = repo.findAll(ClienteFiltros.de(buscar, desde, hasta), pageable);
        Map<UUID, BigDecimal> saldos =
                saldos(pagina.getContent().stream().map(Cliente::getId).toList());
        return pagina.map(c -> toResponse(c, saldos.getOrDefault(c.getId(), BigDecimal.ZERO)));
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtener(UUID id) {
        return toResponse(buscar(id));
    }

    /** Cliente dueno de ese telefono (al crear un pedido se rellenan sus datos). */
    @Transactional(readOnly = true)
    public ClienteResponse porTelefono(String telefono) {
        return repo.findByTelefono(telefono.trim())
                .map(this::toResponse)
                .orElseThrow(
                        () ->
                                new RecursoNoEncontradoException(
                                        CodigoError.CLIENTE_NO_ENCONTRADO,
                                        "No hay un cliente con ese teléfono."));
    }

    public ClienteResponse actualizar(UUID id, long version, ActualizarClienteRequest req) {
        Cliente c = buscar(id);
        IfMatch.verificar(version, c.getVersion());
        String telefono = req.telefono().trim();
        if (repo.existsByTelefonoAndIdNot(telefono, id)) {
            throw telefonoEnUso(telefono);
        }
        c.setNombre(req.nombre().trim());
        c.setApellido(req.apellido().trim());
        c.setTelefono(telefono);
        // saveAndFlush: la version nueva viaja en la respuesta.
        return toResponse(repo.saveAndFlush(c));
    }

    public void eliminar(UUID id) {
        Cliente c = buscar(id);
        if (repo.tienePedidosActivos(id)) {
            throw new ReglaNegocioException(
                    CodigoError.CLIENTE_CON_PEDIDOS_ACTIVOS,
                    "No se puede eliminar: el cliente tiene pedidos sin entregar o con saldo"
                            + " pendiente.");
        }
        // Soft-delete: @SoftDelete convierte este delete en un UPDATE eliminado=true.
        repo.delete(c);
    }

    private Cliente buscar(UUID id) {
        return repo.findById(id)
                .orElseThrow(
                        () ->
                                new RecursoNoEncontradoException(
                                        CodigoError.CLIENTE_NO_ENCONTRADO,
                                        "Cliente no encontrado."));
    }

    private ConflictoException telefonoEnUso(String telefono) {
        return new ConflictoException(
                CodigoError.TELEFONO_EN_USO,
                "El teléfono " + telefono + " ya pertenece a otro cliente.");
    }

    /** Saldo pendiente por cliente (los que no deben no aparecen). */
    private Map<UUID, BigDecimal> saldos(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<UUID, BigDecimal> saldos = new HashMap<>();
        for (Object[] fila : repo.saldosPendientes(ids)) {
            saldos.put((UUID) fila[0], (BigDecimal) fila[1]);
        }
        return saldos;
    }

    private ClienteResponse toResponse(Cliente c) {
        return toResponse(c, saldos(List.of(c.getId())).getOrDefault(c.getId(), BigDecimal.ZERO));
    }

    private static ClienteResponse toResponse(Cliente c, BigDecimal saldo) {
        return new ClienteResponse(
                c.getId(),
                c.getNombre(),
                c.getApellido(),
                c.getTelefono(),
                c.getFechaRegistro(),
                saldo,
                c.getVersion());
    }
}
