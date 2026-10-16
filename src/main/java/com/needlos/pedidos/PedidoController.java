package com.needlos.pedidos;

import com.needlos.common.web.Idempotencia;
import com.needlos.common.web.IfMatch;
import com.needlos.common.web.PaginaResponse;
import com.needlos.pedidos.PedidoFiltros.Criterios;
import com.needlos.pedidos.domain.EstadoPedido;
import com.needlos.pedidos.dto.PedidoDtos.AbonoRequest;
import com.needlos.pedidos.dto.PedidoDtos.ActualizarPedidoRequest;
import com.needlos.pedidos.dto.PedidoDtos.AnularPedidoRequest;
import com.needlos.pedidos.dto.PedidoDtos.AsignarSastreRequest;
import com.needlos.pedidos.dto.PedidoDtos.CambiarEstadoRequest;
import com.needlos.pedidos.dto.PedidoDtos.CrearPedidoRequest;
import com.needlos.pedidos.dto.PedidoDtos.EntregarRequest;
import com.needlos.pedidos.dto.PedidoDtos.PedidoResponse;
import com.needlos.pedidos.dto.PedidoDtos.PedidoResumenResponse;
import com.needlos.pedidos.dto.PedidoDtos.ResumenPedidosResponse;
import com.needlos.pedidos.dto.PedidoDtos.TicketResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Pedidos. Ver: todo el equipo. Crear, cambiar estados, entregar, asignar sastre y registrar
 * abonos: segun los permisos que otorga el SASTRE_ADMIN. Editar y anular: solo SASTRE_ADMIN.
 */
@Tag(name = "Pedidos")
@SecurityRequirement(name = "Bearer")
@RestController
@RequestMapping("/api/v1/pedidos")
@PreAuthorize("hasAnyRole('SASTRE', 'SASTRE_ADMIN')")
public class PedidoController {

    private final PedidoService service;
    private final AbonoService abonos;

    public PedidoController(PedidoService service, AbonoService abonos) {
        this.service = service;
        this.abonos = abonos;
    }

    @Operation(
            summary = "Crea un pedido con sus prendas",
            description =
                    "Cliente existente (clienteId) o nuevo (clienteNuevo), en la misma transacción."
                            + " Abono inicial opcional. Exige Idempotency-Key: si se repite,"
                            + " devuelve el pedido ya creado.")
    @PostMapping
    @PreAuthorize("@permisos.tiene('CREAR_PEDIDOS')")
    public ResponseEntity<PedidoResponse> crear(
            @RequestHeader(value = Idempotencia.CABECERA, required = false) String clave,
            @Valid @RequestBody CrearPedidoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.crear(req, Idempotencia.clave(clave)));
    }

    @Operation(
            summary = "Lista pedidos (paginado y filtrable)",
            description =
                    "Filtros opcionales: buscar (número #G-L o L, nombre/apellido/teléfono del"
                            + " cliente), estado, clienteId, sastreId, entregaDesde, entregaHasta,"
                            + " vencidos, conSaldo, sinEntregar. Ordenable por: numero, fecha, fechaEntrega.")
    @GetMapping
    public PaginaResponse<PedidoResumenResponse> listar(
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) EstadoPedido estado,
            @RequestParam(required = false) UUID clienteId,
            @RequestParam(required = false) UUID sastreId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate entregaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate entregaHasta,
            @RequestParam(defaultValue = "false") boolean vencidos,
            @RequestParam(defaultValue = "false") boolean conSaldo,
            @RequestParam(defaultValue = "false") boolean sinEntregar,
            Pageable pageable) {
        Criterios criterios =
                new Criterios(
                        buscar,
                        estado,
                        clienteId,
                        sastreId,
                        entregaDesde,
                        entregaHasta,
                        vencidos,
                        conSaldo,
                        sinEntregar);
        return PaginaResponse.de(service.listar(criterios, pageable));
    }

    @Operation(summary = "Contadores: en proceso, listos para entregar, vencidos y para hoy")
    @GetMapping("/resumen")
    public ResumenPedidosResponse resumen() {
        return service.resumen();
    }

    @Operation(summary = "Detalle de un pedido (prendas, historial y abonos)")
    @GetMapping("/{id}")
    public PedidoResponse obtener(@PathVariable UUID id) {
        return service.obtener(id);
    }

    @Operation(
            summary = "Edita un pedido (solo SASTRE_ADMIN)",
            description =
                    "Fecha de entrega, descuento y prendas no entregadas. Exige If-Match con la"
                            + " versión leída.")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SASTRE_ADMIN')")
    public PedidoResponse actualizar(
            @PathVariable UUID id,
            @RequestHeader(value = IfMatch.CABECERA, required = false) String ifMatch,
            @Valid @RequestBody ActualizarPedidoRequest req) {
        return service.actualizar(id, IfMatch.version(ifMatch), req);
    }

    @Operation(
            summary = "Cambia el estado de una prenda",
            description =
                    "EN_PROCESO ↔ FINALIZADO (permiso CAMBIAR_ESTADO_PRENDAS) y FINALIZADO →"
                            + " ENTREGADO (permiso ENTREGAR_PRENDAS).")
    @PatchMapping("/{pedidoId}/prendas/{prendaId}/estado")
    public PedidoResponse cambiarEstado(
            @PathVariable UUID pedidoId,
            @PathVariable UUID prendaId,
            @Valid @RequestBody CambiarEstadoRequest req) {
        return service.cambiarEstadoPrenda(pedidoId, prendaId, req.estado());
    }

    @Operation(summary = "Asigna, cambia o quita el sastre de una prenda")
    @PatchMapping("/{pedidoId}/prendas/{prendaId}/sastre")
    @PreAuthorize("@permisos.tiene('ASIGNAR_SASTRE')")
    public PedidoResponse asignarSastre(
            @PathVariable UUID pedidoId,
            @PathVariable UUID prendaId,
            @RequestBody AsignarSastreRequest req) {
        return service.asignarSastre(pedidoId, prendaId, req.sastreId());
    }

    @Operation(summary = "Entrega varias prendas finalizadas a la vez")
    @PatchMapping("/{id}/entregar")
    @PreAuthorize("@permisos.tiene('ENTREGAR_PRENDAS')")
    public PedidoResponse entregar(@PathVariable UUID id, @Valid @RequestBody EntregarRequest req) {
        return service.entregar(id, req.prendaIds());
    }

    @Operation(
            summary = "Anula un pedido (solo SASTRE_ADMIN, con razón)",
            description = "No se puede si tiene abonos vigentes: primero se anulan (devolución).")
    @PatchMapping("/{id}/anular")
    @PreAuthorize("hasRole('SASTRE_ADMIN')")
    public PedidoResponse anular(
            @PathVariable UUID id, @Valid @RequestBody AnularPedidoRequest req) {
        return service.anular(id, req.razon());
    }

    @Operation(
            summary = "Registra un abono",
            description =
                    "Nunca supera el saldo. Exige Idempotency-Key: si se repite, no cobra dos"
                            + " veces.")
    @PostMapping("/{id}/abonos")
    @PreAuthorize("@permisos.tiene('REGISTRAR_ABONOS')")
    public ResponseEntity<PedidoResponse> registrarAbono(
            @PathVariable UUID id,
            @RequestHeader(value = Idempotencia.CABECERA, required = false) String clave,
            @Valid @RequestBody AbonoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(abonos.registrar(id, req, Idempotencia.clave(clave)));
    }

    @Operation(summary = "Datos del ticket del pedido (para ver e imprimir)")
    @GetMapping("/{id}/ticket")
    public TicketResponse ticket(@PathVariable UUID id) {
        return service.ticket(id);
    }
}
