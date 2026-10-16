package com.needlos.pedidos;

import com.needlos.clientes.ClienteRepository;
import com.needlos.clientes.ClienteService;
import com.needlos.common.consecutivo.GeneradorConsecutivo;
import com.needlos.common.consecutivo.TipoConsecutivo;
import com.needlos.common.exception.AccesoDenegadoException;
import com.needlos.common.exception.CodigoError;
import com.needlos.common.exception.RecursoNoEncontradoException;
import com.needlos.common.exception.ReglaNegocioException;
import com.needlos.common.exception.SolicitudInvalidaException;
import com.needlos.common.tenant.TenantContext;
import com.needlos.common.tiempo.Fechas;
import com.needlos.common.web.IfMatch;
import com.needlos.common.web.Ordenamiento;
import com.needlos.empleados.DirectorioEquipo;
import com.needlos.pedidos.PedidoFiltros.Criterios;
import com.needlos.pedidos.domain.EstadoPedido;
import com.needlos.pedidos.domain.EstadoPrenda;
import com.needlos.pedidos.domain.Pedido;
import com.needlos.pedidos.domain.Prenda;
import com.needlos.pedidos.dto.PedidoDtos.AbonoTicket;
import com.needlos.pedidos.dto.PedidoDtos.ActualizarPedidoRequest;
import com.needlos.pedidos.dto.PedidoDtos.CrearPedidoRequest;
import com.needlos.pedidos.dto.PedidoDtos.LineaTicket;
import com.needlos.pedidos.dto.PedidoDtos.PedidoResponse;
import com.needlos.pedidos.dto.PedidoDtos.PedidoResumenResponse;
import com.needlos.pedidos.dto.PedidoDtos.PrendaRequest;
import com.needlos.pedidos.dto.PedidoDtos.ResumenPedidosResponse;
import com.needlos.pedidos.dto.PedidoDtos.SastreriaDelTicket;
import com.needlos.pedidos.dto.PedidoDtos.TicketResponse;
import com.needlos.security.acceso.Permiso;
import com.needlos.security.acceso.VerificadorPermisos;
import com.needlos.tenant.TenantRepository;
import com.needlos.tenant.domain.Tenant;
import com.needlos.tipoprenda.TipoPrendaRepository;
import com.needlos.tipoprenda.domain.TipoPrenda;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pedidos: crear (con cliente existente o nuevo y abono inicial opcional), listar con filtros,
 * detalle, editar (SASTRE_ADMIN), cambiar estado y sastre de las prendas, entregar, anular y
 * ticket. Los permisos generales se exigen en el controlador; aqui solo los que dependen de los
 * datos (p. ej. asignar sastre al crear, o entregar segun el estado pedido).
 */
@Service
@Transactional
public class PedidoService {

    private static final Set<String> ORDENABLES = Set.of("numero", "fecha", "fechaEntrega");

    private final PedidoRepository pedidoRepo;
    private final ClienteRepository clienteRepo;
    private final ClienteService clienteService;
    private final TipoPrendaRepository tipoPrendaRepo;
    private final TenantRepository tenantRepo;
    private final DirectorioEquipo directorio;
    private final VerificadorPermisos permisos;
    private final GeneradorConsecutivo consecutivos;
    private final PedidoMapper mapper;

    public PedidoService(
            PedidoRepository pedidoRepo,
            ClienteRepository clienteRepo,
            ClienteService clienteService,
            TipoPrendaRepository tipoPrendaRepo,
            TenantRepository tenantRepo,
            DirectorioEquipo directorio,
            VerificadorPermisos permisos,
            GeneradorConsecutivo consecutivos,
            PedidoMapper mapper) {
        this.pedidoRepo = pedidoRepo;
        this.clienteRepo = clienteRepo;
        this.clienteService = clienteService;
        this.tipoPrendaRepo = tipoPrendaRepo;
        this.tenantRepo = tenantRepo;
        this.directorio = directorio;
        this.permisos = permisos;
        this.consecutivos = consecutivos;
        this.mapper = mapper;
    }

    // ── Crear ───────────────────────────────────────────────────────

    /**
     * Crea el pedido en una sola transaccion: el cliente nuevo (si aplica), las prendas y el abono
     * inicial. Si algo falla no queda nada a medias. Si la clave ya se uso, devuelve ese pedido.
     */
    public PedidoResponse crear(CrearPedidoRequest req, UUID clave) {
        var existente = pedidoRepo.findByClaveIdempotencia(clave);
        if (existente.isPresent()) {
            return mapper.detalle(existente.get());
        }
        exigirFechaEntregaValida(req.fechaEntrega());
        boolean conSastre = req.prendas().stream().anyMatch(p -> p.sastreId() != null);
        if (conSastre) {
            exigir(Permiso.ASIGNAR_SASTRE, "No tienes permiso para asignar sastres.");
        }
        if (req.abonoInicial() != null) {
            exigir(Permiso.REGISTRAR_ABONOS, "No tienes permiso para registrar abonos.");
        }

        UUID clienteId = resolverCliente(req);
        UUID cuenta = TenantContext.getUsuarioId();
        Map<UUID, TipoPrenda> tipos = tiposActivos(req.prendas(), Set.of());

        Pedido pedido = new Pedido();
        pedido.setClienteId(clienteId);
        pedido.setFecha(Fechas.hoy());
        pedido.setFechaEntrega(req.fechaEntrega());
        pedido.setDescuento(req.descuento() == null ? BigDecimal.ZERO : req.descuento());
        pedido.setClaveIdempotencia(clave);
        for (PrendaRequest p : req.prendas()) {
            validarSastre(p.sastreId());
            pedido.agregarPrenda(nuevaPrenda(p, tipos, cuenta));
        }
        pedido.verificarTotales();
        if (req.abonoInicial() != null) {
            // Misma clave que el pedido: un reintento no puede duplicar el abono.
            pedido.registrarAbono(req.abonoInicial().monto(), req.abonoInicial().metodo(), clave);
        }

        // Los numeros se toman al final: si algo fallo antes, no se consume el local.
        pedido.setNumero(consecutivos.siguiente(TipoConsecutivo.PEDIDO));
        pedido.setNumeroGlobal(consecutivos.siguienteGlobal(TipoConsecutivo.PEDIDO));
        return mapper.detalle(pedidoRepo.saveAndFlush(pedido));
    }

    // ── Consultar ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<PedidoResumenResponse> listar(Criterios criterios, Pageable pageable) {
        Ordenamiento.validar(pageable, ORDENABLES);
        LocalDate desde = criterios.entregaDesde();
        LocalDate hasta = criterios.entregaHasta();
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new SolicitudInvalidaException(
                    CodigoError.SOLICITUD_INVALIDA,
                    "La fecha inicial no puede ser posterior a la final.");
        }
        Page<Pedido> pagina =
                pedidoRepo.findAll(PedidoFiltros.de(criterios, Fechas.hoy()), pageable);
        return new PageImpl<>(
                mapper.resumenes(pagina.getContent()), pageable, pagina.getTotalElements());
    }

    /** Contadores de los tiles: en proceso, listos para entregar, vencidos y para hoy. */
    @Transactional(readOnly = true)
    public ResumenPedidosResponse resumen() {
        LocalDate hoy = Fechas.hoy();
        return new ResumenPedidosResponse(
                pedidoRepo.count(PedidoFiltros.estado(EstadoPedido.EN_PROCESO)),
                pedidoRepo.count(PedidoFiltros.estado(EstadoPedido.FINALIZADO)),
                pedidoRepo.count(PedidoFiltros.vencidos(hoy)),
                pedidoRepo.count(PedidoFiltros.paraEntregarEl(hoy)));
    }

    @Transactional(readOnly = true)
    public PedidoResponse obtener(UUID id) {
        return mapper.detalle(buscar(id));
    }

    // ── Editar (SASTRE_ADMIN) ───────────────────────────────────────

    /**
     * Edita fecha de entrega, descuento y prendas. Las prendas con id se actualizan (salvo las
     * entregadas, que no cambian), las nuevas se agregan EN_PROCESO y las que faltan se quitan (no
     * se puede quitar una entregada). El cliente no se cambia.
     */
    public PedidoResponse actualizar(UUID id, long version, ActualizarPedidoRequest req) {
        Pedido pedido = buscar(id);
        IfMatch.verificar(version, pedido.getVersion());
        pedido.verificarModificable();
        if (!req.fechaEntrega().equals(pedido.getFechaEntrega())) {
            exigirFechaEntregaValida(req.fechaEntrega());
        }

        Map<UUID, Prenda> actuales =
                pedido.getPrendas().stream()
                        .collect(Collectors.toMap(Prenda::getId, Function.identity()));
        Set<UUID> tiposActuales =
                pedido.getPrendas().stream()
                        .map(Prenda::getTipoPrendaId)
                        .collect(Collectors.toSet());
        Map<UUID, TipoPrenda> tipos = tiposActivos(req.prendas(), tiposActuales);
        UUID cuenta = TenantContext.getUsuarioId();
        Set<UUID> conservadas = new HashSet<>();

        for (PrendaRequest p : req.prendas()) {
            if (p.id() == null) {
                validarSastre(p.sastreId());
                pedido.agregarPrenda(nuevaPrenda(p, tipos, cuenta));
                continue;
            }
            Prenda prenda = actuales.get(p.id());
            if (prenda == null) {
                throw prendaNoEncontrada();
            }
            conservadas.add(prenda.getId());
            if (prenda.entregada()) {
                if (cambia(prenda, p)) {
                    throw new ReglaNegocioException(
                            CodigoError.PRENDA_ENTREGADA,
                            "Una prenda entregada no se puede modificar.");
                }
                continue;
            }
            if (!Objects.equals(prenda.getSastreId(), p.sastreId())) {
                validarSastre(p.sastreId());
            }
            prenda.setTipoPrendaId(p.tipoPrendaId());
            prenda.setSastreId(p.sastreId());
            prenda.setCantidad(p.cantidad());
            prenda.setDescripcion(p.descripcion().trim());
            prenda.setPrecioUnitario(p.precioUnitario());
        }

        for (Prenda prenda : List.copyOf(actuales.values())) {
            if (!conservadas.contains(prenda.getId())) {
                if (prenda.entregada()) {
                    throw new ReglaNegocioException(
                            CodigoError.PRENDA_ENTREGADA,
                            "Una prenda entregada no se puede quitar del pedido.");
                }
                pedido.getPrendas().remove(prenda);
            }
        }

        pedido.setFechaEntrega(req.fechaEntrega());
        pedido.setDescuento(req.descuento() == null ? BigDecimal.ZERO : req.descuento());
        pedido.verificarTotales();
        return mapper.detalle(pedidoRepo.saveAndFlush(pedido));
    }

    // ── Prendas ─────────────────────────────────────────────────────

    /**
     * Cambia el estado de una prenda. Entregar exige ENTREGAR_PRENDAS; pasar entre en proceso y
     * finalizado exige CAMBIAR_ESTADO_PRENDAS.
     */
    public PedidoResponse cambiarEstadoPrenda(UUID pedidoId, UUID prendaId, EstadoPrenda nuevo) {
        if (nuevo == EstadoPrenda.ENTREGADO) {
            exigir(Permiso.ENTREGAR_PRENDAS, "No tienes permiso para entregar prendas.");
        } else {
            exigir(
                    Permiso.CAMBIAR_ESTADO_PRENDAS,
                    "No tienes permiso para cambiar el estado de las prendas.");
        }
        Pedido pedido = buscar(pedidoId);
        pedido.verificarModificable();
        prenda(pedido, prendaId).cambiarEstado(nuevo, TenantContext.getUsuarioId());
        return mapper.detalle(pedidoRepo.saveAndFlush(pedido));
    }

    /** Entrega varias prendas a la vez (todas deben estar finalizadas). */
    public PedidoResponse entregar(UUID pedidoId, List<UUID> prendaIds) {
        Pedido pedido = buscar(pedidoId);
        pedido.verificarModificable();
        UUID cuenta = TenantContext.getUsuarioId();
        for (UUID prendaId : Set.copyOf(prendaIds)) {
            prenda(pedido, prendaId).cambiarEstado(EstadoPrenda.ENTREGADO, cuenta);
        }
        return mapper.detalle(pedidoRepo.saveAndFlush(pedido));
    }

    /** Asigna, cambia o quita (null) el sastre de una prenda no entregada. */
    public PedidoResponse asignarSastre(UUID pedidoId, UUID prendaId, UUID sastreId) {
        Pedido pedido = buscar(pedidoId);
        pedido.verificarModificable();
        Prenda prenda = prenda(pedido, prendaId);
        if (prenda.entregada()) {
            throw new ReglaNegocioException(
                    CodigoError.PRENDA_ENTREGADA, "La prenda ya fue entregada.");
        }
        validarSastre(sastreId);
        prenda.setSastreId(sastreId);
        return mapper.detalle(pedidoRepo.saveAndFlush(pedido));
    }

    // ── Anular (SASTRE_ADMIN) ───────────────────────────────────────

    public PedidoResponse anular(UUID pedidoId, String razon) {
        Pedido pedido = buscar(pedidoId);
        pedido.anular(razon);
        return mapper.detalle(pedidoRepo.saveAndFlush(pedido));
    }

    // ── Ticket ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TicketResponse ticket(UUID pedidoId) {
        Pedido p = buscar(pedidoId);
        Tenant t =
                tenantRepo
                        .findById(TenantContext.getTenantId())
                        .orElseThrow(
                                () ->
                                        new RecursoNoEncontradoException(
                                                CodigoError.SASTRERIA_NO_ENCONTRADA,
                                                "Sastrería no encontrada."));
        Map<UUID, String> tipos =
                mapper.tipos(p.getPrendas().stream().map(Prenda::getTipoPrendaId).toList());
        return new TicketResponse(
                new SastreriaDelTicket(
                        t.getNombre(), t.getNit(), t.getDireccion(), t.getTelefono()),
                p.numeroVisible(),
                p.getFecha(),
                p.getFechaEntrega(),
                mapper.clientes(List.of(p.getClienteId())).get(p.getClienteId()),
                p.getPrendas().stream()
                        .map(
                                pr ->
                                        new LineaTicket(
                                                tipos.get(pr.getTipoPrendaId()),
                                                pr.getDescripcion(),
                                                pr.getCantidad(),
                                                pr.getPrecioUnitario(),
                                                pr.subtotal()))
                        .toList(),
                p.subtotal(),
                p.getDescuento(),
                p.total(),
                p.getAbonos().stream()
                        .filter(a -> !a.isAnulado())
                        .map(a -> new AbonoTicket(a.getCreadoEn(), a.getMetodo(), a.getMonto()))
                        .toList(),
                p.abonado(),
                p.saldo(),
                p.isAnulado(),
                Instant.now());
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private UUID resolverCliente(CrearPedidoRequest req) {
        boolean existente = req.clienteId() != null;
        boolean nuevo = req.clienteNuevo() != null;
        if (existente == nuevo) {
            throw new SolicitudInvalidaException(
                    CodigoError.CLIENTE_REQUERIDO,
                    "Indica el cliente del pedido: uno existente o los datos de uno nuevo.");
        }
        if (nuevo) {
            return clienteService.crear(req.clienteNuevo()).id();
        }
        if (!clienteRepo.existsById(req.clienteId())) {
            throw new RecursoNoEncontradoException(
                    CodigoError.CLIENTE_NO_ENCONTRADO, "Cliente no encontrado.");
        }
        return req.clienteId();
    }

    /**
     * Tipos de prenda de las lineas, validando que existan y esten activos. Los de {@code yaUsados}
     * (prendas existentes al editar) se aceptan aunque se hayan desactivado despues.
     */
    private Map<UUID, TipoPrenda> tiposActivos(List<PrendaRequest> lineas, Set<UUID> yaUsados) {
        Set<UUID> ids =
                lineas.stream().map(PrendaRequest::tipoPrendaId).collect(Collectors.toSet());
        Map<UUID, TipoPrenda> tipos =
                tipoPrendaRepo.findAllById(ids).stream()
                        .collect(Collectors.toMap(TipoPrenda::getId, Function.identity()));
        for (UUID id : ids) {
            TipoPrenda tipo = tipos.get(id);
            if (tipo == null) {
                throw new RecursoNoEncontradoException(
                        CodigoError.TIPO_PRENDA_NO_ENCONTRADO, "Tipo de prenda no encontrado.");
            }
            if (!tipo.isActivo() && !yaUsados.contains(id)) {
                throw new ReglaNegocioException(
                        CodigoError.TIPO_PRENDA_INACTIVO,
                        "El tipo de prenda \"" + tipo.getNombre() + "\" está desactivado.");
            }
        }
        return tipos;
    }

    private static Prenda nuevaPrenda(PrendaRequest p, Map<UUID, TipoPrenda> tipos, UUID cuenta) {
        Prenda prenda = new Prenda();
        prenda.setTipoPrendaId(tipos.get(p.tipoPrendaId()).getId());
        prenda.setSastreId(p.sastreId());
        prenda.setCantidad(p.cantidad());
        prenda.setDescripcion(p.descripcion().trim());
        prenda.setPrecioUnitario(p.precioUnitario());
        prenda.setEstado(EstadoPrenda.EN_PROCESO);
        prenda.registrarEnHistorial(EstadoPrenda.EN_PROCESO, cuenta);
        return prenda;
    }

    private static boolean cambia(Prenda prenda, PrendaRequest p) {
        return !prenda.getTipoPrendaId().equals(p.tipoPrendaId())
                || !Objects.equals(prenda.getSastreId(), p.sastreId())
                || prenda.getCantidad() != p.cantidad()
                || !prenda.getDescripcion().equals(p.descripcion().trim())
                || prenda.getPrecioUnitario().compareTo(p.precioUnitario()) != 0;
    }

    private void validarSastre(UUID sastreId) {
        if (sastreId != null && !directorio.esMiembroActivo(sastreId)) {
            throw new ReglaNegocioException(
                    CodigoError.SASTRE_INVALIDO,
                    "El sastre indicado no es un miembro activo de la sastrería.");
        }
    }

    private static void exigirFechaEntregaValida(LocalDate fecha) {
        if (fecha.isBefore(Fechas.hoy())) {
            throw new ReglaNegocioException(
                    CodigoError.FECHA_ENTREGA_INVALIDA,
                    "La fecha de entrega no puede ser anterior a hoy.");
        }
    }

    private void exigir(Permiso permiso, String mensaje) {
        if (!permisos.tiene(permiso)) {
            throw new AccesoDenegadoException(CodigoError.SIN_PERMISO, mensaje);
        }
    }

    private Pedido buscar(UUID id) {
        return pedidoRepo
                .findWithPrendasById(id)
                .orElseThrow(
                        () ->
                                new RecursoNoEncontradoException(
                                        CodigoError.PEDIDO_NO_ENCONTRADO, "Pedido no encontrado."));
    }

    private static Prenda prenda(Pedido pedido, UUID prendaId) {
        return pedido.getPrendas().stream()
                .filter(p -> p.getId().equals(prendaId))
                .findFirst()
                .orElseThrow(PedidoService::prendaNoEncontrada);
    }

    private static RecursoNoEncontradoException prendaNoEncontrada() {
        return new RecursoNoEncontradoException(
                CodigoError.PRENDA_NO_ENCONTRADA, "Prenda no encontrada en el pedido.");
    }
}
