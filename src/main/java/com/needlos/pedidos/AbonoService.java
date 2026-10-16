package com.needlos.pedidos;

import com.needlos.common.exception.CodigoError;
import com.needlos.common.exception.RecursoNoEncontradoException;
import com.needlos.common.tenant.TenantContext;
import com.needlos.pedidos.domain.Abono;
import com.needlos.pedidos.domain.Pedido;
import com.needlos.pedidos.dto.PedidoDtos.AbonoRequest;
import com.needlos.pedidos.dto.PedidoDtos.AnularAbonoRequest;
import com.needlos.pedidos.dto.PedidoDtos.PedidoResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abonos de un pedido. Un abono nunca supera el saldo; el pedido queda pagado solo cuando los
 * abonos cubren el total. Anular (solo SASTRE_ADMIN) no borra: deja motivo y razon, y el abono deja
 * de contar. Ambas operaciones devuelven el pedido actualizado (saldo, pagado).
 */
@Service
@Transactional
public class AbonoService {

    private final PedidoRepository pedidoRepo;
    private final AbonoRepository abonoRepo;
    private final PedidoMapper mapper;

    public AbonoService(
            PedidoRepository pedidoRepo, AbonoRepository abonoRepo, PedidoMapper mapper) {
        this.pedidoRepo = pedidoRepo;
        this.abonoRepo = abonoRepo;
        this.mapper = mapper;
    }

    /** Registra un abono. Si la clave ya se uso, no registra otro y devuelve el pedido. */
    public PedidoResponse registrar(UUID pedidoId, AbonoRequest req, UUID clave) {
        var repetido = abonoRepo.findByClaveIdempotencia(clave);
        if (repetido.isPresent()) {
            return mapper.detalle(repetido.get().getPedido());
        }
        // Bloquea la fila del pedido: dos abonos simultaneos no superan juntos el saldo.
        Pedido pedido =
                pedidoRepo
                        .bloquear(pedidoId)
                        .orElseThrow(
                                () ->
                                        new RecursoNoEncontradoException(
                                                CodigoError.PEDIDO_NO_ENCONTRADO,
                                                "Pedido no encontrado."));
        pedido.registrarAbono(req.monto(), req.metodo(), clave);
        return mapper.detalle(pedidoRepo.saveAndFlush(pedido));
    }

    public PedidoResponse anular(UUID abonoId, AnularAbonoRequest req) {
        Abono abono =
                abonoRepo
                        .findById(abonoId)
                        .orElseThrow(
                                () ->
                                        new RecursoNoEncontradoException(
                                                CodigoError.ABONO_NO_ENCONTRADO,
                                                "Abono no encontrado."));
        abono.anular(req.motivo(), req.razon(), TenantContext.getUsuarioId());
        abonoRepo.saveAndFlush(abono);
        return mapper.detalle(abono.getPedido());
    }
}
