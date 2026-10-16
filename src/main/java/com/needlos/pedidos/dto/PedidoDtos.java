package com.needlos.pedidos.dto;

import com.needlos.clientes.dto.ClienteDtos.CrearClienteRequest;
import com.needlos.pedidos.domain.EstadoPedido;
import com.needlos.pedidos.domain.EstadoPrenda;
import com.needlos.pedidos.domain.MetodoPago;
import com.needlos.pedidos.domain.MotivoAnulacionAbono;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class PedidoDtos {

    private PedidoDtos() {}

    // ── Peticiones ──────────────────────────────────────────────────

    /**
     * Nuevo pedido. El cliente va por {@code clienteId} (ya existe) o por {@code clienteNuevo} (se
     * crea en la misma transaccion); exactamente uno de los dos.
     */
    public record CrearPedidoRequest(
            UUID clienteId,
            @Valid CrearClienteRequest clienteNuevo,
            @NotNull LocalDate fechaEntrega,
            @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal descuento,
            @NotEmpty @Size(max = 50) @Valid List<PrendaRequest> prendas,
            @Valid AbonoRequest abonoInicial) {}

    /**
     * Edicion (solo SASTRE_ADMIN). Las prendas con id se actualizan, sin id se agregan y las que
     * faltan se quitan.
     */
    public record ActualizarPedidoRequest(
            @NotNull LocalDate fechaEntrega,
            @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal descuento,
            @NotEmpty @Size(max = 50) @Valid List<PrendaRequest> prendas) {}

    /** {@code id}: solo al editar, para una prenda existente. */
    public record PrendaRequest(
            UUID id,
            @NotNull UUID tipoPrendaId,
            UUID sastreId,
            @Min(1) @Max(999) int cantidad,
            @NotBlank @Size(min = 3, max = 200) String descripcion,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 10, fraction = 2)
                    BigDecimal precioUnitario) {}

    public record CambiarEstadoRequest(@NotNull EstadoPrenda estado) {}

    /** {@code sastreId} null = dejar sin asignar. */
    public record AsignarSastreRequest(UUID sastreId) {}

    public record EntregarRequest(@NotEmpty @Size(max = 50) List<@NotNull UUID> prendaIds) {}

    public record AnularPedidoRequest(@NotBlank @Size(min = 3, max = 300) String razon) {}

    public record AbonoRequest(
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 10, fraction = 2)
                    BigDecimal monto,
            @NotNull MetodoPago metodo) {}

    public record AnularAbonoRequest(
            @NotNull MotivoAnulacionAbono motivo,
            @NotBlank @Size(min = 3, max = 300) String razon) {}

    // ── Respuestas ──────────────────────────────────────────────────

    public record ClienteDelPedido(UUID id, String nombre, String apellido, String telefono) {}

    /** Fila del listado. */
    public record PedidoResumenResponse(
            UUID id,
            String numero,
            ClienteDelPedido cliente,
            LocalDate fecha,
            LocalDate fechaEntrega,
            EstadoPedido estado,
            int prendas,
            BigDecimal total,
            BigDecimal saldo,
            boolean vencido) {}

    /** Detalle completo. {@code version} va en If-Match al editar. */
    public record PedidoResponse(
            UUID id,
            String numero,
            long numeroGlobal,
            long numeroLocal,
            ClienteDelPedido cliente,
            LocalDate fecha,
            LocalDate fechaEntrega,
            EstadoPedido estado,
            BigDecimal subtotal,
            BigDecimal descuento,
            BigDecimal total,
            BigDecimal abonado,
            BigDecimal saldo,
            boolean pagado,
            boolean vencido,
            String razonAnulacion,
            List<PrendaResponse> prendas,
            List<AbonoResponse> abonos,
            long version) {}

    public record PrendaResponse(
            UUID id,
            UUID tipoPrendaId,
            String tipoPrenda,
            UUID sastreId,
            String sastre,
            int cantidad,
            String descripcion,
            BigDecimal precioUnitario,
            BigDecimal subtotal,
            EstadoPrenda estado,
            List<HistorialResponse> historial) {}

    public record HistorialResponse(EstadoPrenda estado, Instant fecha, String usuario) {}

    public record AbonoResponse(
            UUID id,
            BigDecimal monto,
            MetodoPago metodo,
            Instant fecha,
            String registradoPor,
            boolean anulado,
            MotivoAnulacionAbono motivoAnulacion,
            String razonAnulacion,
            Instant anuladoEn,
            String anuladoPor) {}

    /** Contadores para los tiles de la seccion Pedidos. */
    public record ResumenPedidosResponse(
            long enProceso, long listosParaEntregar, long vencidos, long paraEntregarHoy) {}

    // ── Ticket ──────────────────────────────────────────────────────

    public record TicketResponse(
            SastreriaDelTicket sastreria,
            String numero,
            LocalDate fecha,
            LocalDate fechaEntrega,
            ClienteDelPedido cliente,
            List<LineaTicket> prendas,
            BigDecimal subtotal,
            BigDecimal descuento,
            BigDecimal total,
            List<AbonoTicket> abonos,
            BigDecimal abonado,
            BigDecimal saldo,
            boolean anulado,
            Instant emitido) {}

    public record SastreriaDelTicket(
            String nombre, String nit, String direccion, String telefono) {}

    public record LineaTicket(
            String tipoPrenda,
            String descripcion,
            int cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal) {}

    public record AbonoTicket(Instant fecha, MetodoPago metodo, BigDecimal monto) {}
}
