package com.needlos.pedidos;

import com.needlos.clientes.ClienteRepository;
import com.needlos.clientes.domain.Cliente;
import com.needlos.common.tiempo.Fechas;
import com.needlos.empleados.DirectorioEquipo;
import com.needlos.pedidos.domain.Abono;
import com.needlos.pedidos.domain.EstadoPrendaHistorial;
import com.needlos.pedidos.domain.Pedido;
import com.needlos.pedidos.domain.Prenda;
import com.needlos.pedidos.dto.PedidoDtos.AbonoResponse;
import com.needlos.pedidos.dto.PedidoDtos.ClienteDelPedido;
import com.needlos.pedidos.dto.PedidoDtos.HistorialResponse;
import com.needlos.pedidos.dto.PedidoDtos.PedidoResponse;
import com.needlos.pedidos.dto.PedidoDtos.PedidoResumenResponse;
import com.needlos.pedidos.dto.PedidoDtos.PrendaResponse;
import com.needlos.tipoprenda.TipoPrendaRepository;
import com.needlos.tipoprenda.domain.TipoPrenda;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * Convierte pedidos a DTOs resolviendo en lote los nombres que viven en otros modulos (cliente,
 * tipo de prenda, sastre, quien hizo cada cambio): una consulta por tipo de dato, nunca una por
 * fila.
 */
@Component
class PedidoMapper {

    private final ClienteRepository clienteRepo;
    private final TipoPrendaRepository tipoPrendaRepo;
    private final DirectorioEquipo directorio;

    PedidoMapper(
            ClienteRepository clienteRepo,
            TipoPrendaRepository tipoPrendaRepo,
            DirectorioEquipo directorio) {
        this.clienteRepo = clienteRepo;
        this.tipoPrendaRepo = tipoPrendaRepo;
        this.directorio = directorio;
    }

    List<PedidoResumenResponse> resumenes(List<Pedido> pedidos) {
        Map<UUID, ClienteDelPedido> clientes =
                clientes(pedidos.stream().map(Pedido::getClienteId).toList());
        LocalDate hoy = Fechas.hoy();
        return pedidos.stream()
                .map(
                        p ->
                                new PedidoResumenResponse(
                                        p.getId(),
                                        p.numeroVisible(),
                                        clientes.get(p.getClienteId()),
                                        p.getFecha(),
                                        p.getFechaEntrega(),
                                        p.estado(),
                                        p.getPrendas().size(),
                                        p.total(),
                                        p.saldo(),
                                        p.vencido(hoy)))
                .toList();
    }

    PedidoResponse detalle(Pedido p) {
        ClienteDelPedido cliente = clientes(List.of(p.getClienteId())).get(p.getClienteId());
        Map<UUID, String> tipos =
                tipos(p.getPrendas().stream().map(Prenda::getTipoPrendaId).toList());
        Map<UUID, String> sastres =
                directorio.nombresDeAccesos(
                        p.getPrendas().stream()
                                .map(Prenda::getSastreId)
                                .filter(Objects::nonNull)
                                .toList());
        Set<UUID> cuentas = new HashSet<>();
        p.getPrendas().stream()
                .flatMap(pr -> pr.getHistorial().stream())
                .map(EstadoPrendaHistorial::getUsuarioId)
                .filter(Objects::nonNull)
                .forEach(cuentas::add);
        p.getAbonos().stream()
                .flatMap(a -> Stream.of(a.getCreadoPor(), a.getAnuladoPor()))
                .filter(Objects::nonNull)
                .forEach(cuentas::add);
        Map<UUID, String> nombres = directorio.nombresDeCuentas(cuentas);

        List<PrendaResponse> prendas =
                p.getPrendas().stream()
                        .map(
                                pr ->
                                        new PrendaResponse(
                                                pr.getId(),
                                                pr.getTipoPrendaId(),
                                                tipos.get(pr.getTipoPrendaId()),
                                                pr.getSastreId(),
                                                pr.getSastreId() == null
                                                        ? null
                                                        : sastres.get(pr.getSastreId()),
                                                pr.getCantidad(),
                                                pr.getDescripcion(),
                                                pr.getPrecioUnitario(),
                                                pr.subtotal(),
                                                pr.getEstado(),
                                                pr.getHistorial().stream()
                                                        .map(
                                                                h ->
                                                                        new HistorialResponse(
                                                                                h.getEstado(),
                                                                                h.getFechaCambio(),
                                                                                nombre(
                                                                                        nombres,
                                                                                        h
                                                                                                .getUsuarioId())))
                                                        .toList()))
                        .toList();
        List<AbonoResponse> abonos = p.getAbonos().stream().map(a -> abono(a, nombres)).toList();

        return new PedidoResponse(
                p.getId(),
                p.numeroVisible(),
                p.getNumeroGlobal(),
                p.getNumero(),
                cliente,
                p.getFecha(),
                p.getFechaEntrega(),
                p.estado(),
                p.subtotal(),
                p.getDescuento(),
                p.total(),
                p.abonado(),
                p.saldo(),
                p.pagado(),
                p.vencido(Fechas.hoy()),
                p.getRazonAnulacion(),
                prendas,
                abonos,
                p.getVersion());
    }

    Map<UUID, ClienteDelPedido> clientes(List<UUID> ids) {
        return clienteRepo.findAllById(Set.copyOf(ids)).stream()
                .collect(Collectors.toMap(Cliente::getId, PedidoMapper::cliente));
    }

    static ClienteDelPedido cliente(Cliente c) {
        return new ClienteDelPedido(c.getId(), c.getNombre(), c.getApellido(), c.getTelefono());
    }

    Map<UUID, String> tipos(List<UUID> ids) {
        return tipoPrendaRepo.findAllById(Set.copyOf(ids)).stream()
                .collect(Collectors.toMap(TipoPrenda::getId, TipoPrenda::getNombre));
    }

    private static AbonoResponse abono(Abono a, Map<UUID, String> nombres) {
        return new AbonoResponse(
                a.getId(),
                a.getMonto(),
                a.getMetodo(),
                a.getCreadoEn(),
                nombre(nombres, a.getCreadoPor()),
                a.isAnulado(),
                a.getMotivoAnulacion(),
                a.getRazonAnulacion(),
                a.getAnuladoEn(),
                nombre(nombres, a.getAnuladoPor()));
    }

    private static String nombre(Map<UUID, String> nombres, UUID cuentaId) {
        return cuentaId == null ? null : nombres.get(cuentaId);
    }
}
