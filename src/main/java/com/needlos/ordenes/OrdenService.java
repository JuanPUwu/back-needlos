package com.needlos.ordenes;

import com.needlos.clientes.ClienteRepository;
import com.needlos.common.consecutivo.GeneradorConsecutivo;
import com.needlos.common.consecutivo.TipoConsecutivo;
import com.needlos.common.exception.CodigoError;
import com.needlos.common.exception.RecursoNoEncontradoException;
import com.needlos.common.exception.ReglaNegocioException;
import com.needlos.common.tenant.TenantContext;
import com.needlos.common.web.Ordenamiento;
import com.needlos.ordenes.domain.EstadoPrenda;
import com.needlos.ordenes.domain.Orden;
import com.needlos.ordenes.domain.Prenda;
import com.needlos.ordenes.dto.OrdenDtos.CrearOrdenRequest;
import com.needlos.ordenes.dto.OrdenDtos.OrdenResponse;
import com.needlos.ordenes.dto.OrdenDtos.PrendaRequest;
import com.needlos.ordenes.dto.OrdenDtos.PrendaResponse;
import com.needlos.tipoprenda.TipoPrendaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class OrdenService {

    private static final Set<String> ORDENABLES = Set.of("numero", "fecha", "fechaEntrega");

    private final OrdenRepository      ordenRepo;
    private final ClienteRepository    clienteRepo;
    private final TipoPrendaRepository tipoPrendaRepo;
    private final GeneradorConsecutivo consecutivos;

    public OrdenService(OrdenRepository ordenRepo,
                        ClienteRepository clienteRepo,
                        TipoPrendaRepository tipoPrendaRepo,
                        GeneradorConsecutivo consecutivos) {
        this.ordenRepo = ordenRepo;
        this.clienteRepo = clienteRepo;
        this.tipoPrendaRepo = tipoPrendaRepo;
        this.consecutivos = consecutivos;
    }

    public OrdenResponse crear(CrearOrdenRequest req) {
        if (!clienteRepo.existsById(req.clienteId())) {
            throw new RecursoNoEncontradoException(CodigoError.CLIENTE_NO_ENCONTRADO, "Cliente no encontrado.");
        }

        UUID usuarioActual = TenantContext.getUsuarioId();

        Orden orden = new Orden();
        orden.setClienteId(req.clienteId());
        orden.setFechaEntrega(req.fechaEntrega());
        orden.setDescuento(req.descuento() == null ? BigDecimal.ZERO : req.descuento());

        for (PrendaRequest p : req.prendas()) {
            if (!tipoPrendaRepo.existsById(p.tipoPrendaId())) {
                throw new RecursoNoEncontradoException(CodigoError.TIPO_PRENDA_NO_ENCONTRADO,
                        "Tipo de prenda no encontrado.");
            }
            Prenda prenda = new Prenda();
            prenda.setTipoPrendaId(p.tipoPrendaId());
            prenda.setSastreId(p.sastreId());
            prenda.setCantidad(p.cantidad());
            prenda.setDescripcion(p.descripcion());
            prenda.setPrecioUnitario(p.precioUnitario());
            prenda.setEstado(EstadoPrenda.EN_PROCESO);
            prenda.registrarEnHistorial(EstadoPrenda.EN_PROCESO, usuarioActual);
            orden.agregarPrenda(prenda);
        }

        // El numero se toma al final: si algo fallo antes, no se consume ninguno.
        orden.setNumero(consecutivos.siguiente(TipoConsecutivo.PEDIDO));
        return toResponse(ordenRepo.save(orden));
    }

    @Transactional(readOnly = true)
    public Page<OrdenResponse> listar(Pageable pageable) {
        Ordenamiento.validar(pageable, ORDENABLES);
        return ordenRepo.findAllBy(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public OrdenResponse obtener(UUID id) {
        return toResponse(buscar(id));
    }

    /** Avanza el estado de una prenda concreta de la orden, con trazabilidad. */
    public OrdenResponse cambiarEstadoPrenda(UUID ordenId, UUID prendaId, EstadoPrenda nuevoEstado) {
        Orden orden = buscar(ordenId);
        if (orden.isAnulada()) {
            throw new ReglaNegocioException(CodigoError.PEDIDO_ANULADO,
                    "No se puede cambiar el estado de un pedido anulado.");
        }
        Prenda prenda = orden.getPrendas().stream()
                .filter(p -> p.getId().equals(prendaId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        CodigoError.PRENDA_NO_ENCONTRADA, "Prenda no encontrada en el pedido."));

        prenda.cambiarEstado(nuevoEstado, TenantContext.getUsuarioId());
        return toResponse(ordenRepo.save(orden));
    }

    /** Anula la orden (exige razon). El permiso SASTRE_ADMIN se valida en el controlador. */
    public OrdenResponse anular(UUID ordenId, String razon) {
        Orden orden = buscar(ordenId);
        orden.anular(razon);
        return toResponse(ordenRepo.save(orden));
    }

    private Orden buscar(UUID id) {
        return ordenRepo.findWithPrendasById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        CodigoError.PEDIDO_NO_ENCONTRADO, "Pedido no encontrado."));
    }

    private OrdenResponse toResponse(Orden o) {
        var prendas = o.getPrendas().stream()
                .map(p -> new PrendaResponse(
                        p.getId(), p.getTipoPrendaId(), p.getSastreId(), p.getCantidad(),
                        p.getDescripcion(), p.getPrecioUnitario(), p.subtotal(), p.getEstado()))
                .toList();

        return new OrdenResponse(
                o.getId(), o.getNumero(), o.getClienteId(), o.getFecha(), o.getFechaEntrega(),
                o.estadoActual(), o.subtotal(), o.getDescuento(), o.total(),
                o.isAnulada(), o.getRazonAnulacion(), prendas);
    }
}
