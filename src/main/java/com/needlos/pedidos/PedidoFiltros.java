package com.needlos.pedidos;

import com.needlos.clientes.domain.Cliente;
import com.needlos.pedidos.domain.Abono;
import com.needlos.pedidos.domain.EstadoPedido;
import com.needlos.pedidos.domain.EstadoPrenda;
import com.needlos.pedidos.domain.Pedido;
import com.needlos.pedidos.domain.Prenda;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros del listado de pedidos (todos opcionales, combinados con AND). El estado del pedido es
 * derivado de sus prendas, asi que se filtra con subconsultas sobre ellas.
 */
final class PedidoFiltros {

    /** "#12-100", "12-100", "#100" o "100". */
    private static final Pattern NUMERO = Pattern.compile("^#?(?:(\\d{1,18})-)?(\\d{1,18})$");

    private PedidoFiltros() {}

    /** Criterios del listado. Null o false = sin filtro. */
    record Criterios(
            String buscar,
            EstadoPedido estado,
            UUID clienteId,
            UUID sastreId,
            LocalDate entregaDesde,
            LocalDate entregaHasta,
            boolean vencidos,
            boolean conSaldo,
            boolean sinEntregar) {}

    static Specification<Pedido> de(Criterios c, LocalDate hoy) {
        return Specification.allOf(
                texto(c.buscar()),
                estado(c.estado()),
                igual("clienteId", c.clienteId()),
                sastre(c.sastreId()),
                entregaDesde(c.entregaDesde()),
                entregaHasta(c.entregaHasta()),
                c.vencidos() ? vencidos(hoy) : Specification.unrestricted(),
                c.conSaldo() ? conSaldo() : Specification.unrestricted(),
                c.sinEntregar() ? sinEntregar() : Specification.unrestricted());
    }

    /** Numero del pedido, o nombre / apellido / telefono del cliente. */
    static Specification<Pedido> texto(String buscar) {
        if (buscar == null || buscar.isBlank()) {
            return Specification.unrestricted();
        }
        String limpio = buscar.trim();
        Matcher numero = NUMERO.matcher(limpio);
        String patron = "%" + limpio.toLowerCase() + "%";
        return (root, query, cb) -> {
            Subquery<UUID> clientes = query.subquery(UUID.class);
            Root<Cliente> cl = clientes.from(Cliente.class);
            clientes.select(cl.get("id"))
                    .where(
                            cb.or(
                                    cb.like(cb.lower(cl.get("nombre")), patron),
                                    cb.like(cb.lower(cl.get("apellido")), patron),
                                    cb.like(cl.get("telefono"), "%" + limpio + "%")));
            Predicate porCliente = root.get("clienteId").in(clientes);
            if (!numero.matches()) {
                return porCliente;
            }
            Predicate porNumero = cb.equal(root.get("numero"), Long.parseLong(numero.group(2)));
            if (numero.group(1) != null) {
                porNumero =
                        cb.and(
                                porNumero,
                                cb.equal(
                                        root.get("numeroGlobal"), Long.parseLong(numero.group(1))));
            }
            return cb.or(porNumero, porCliente);
        };
    }

    static Specification<Pedido> estado(EstadoPedido estado) {
        if (estado == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> {
            Predicate vigente = cb.isFalse(root.get("anulado"));
            return switch (estado) {
                case ANULADO -> cb.isTrue(root.get("anulado"));
                case EN_PROCESO ->
                        cb.and(vigente, hayPrenda(root, query, cb, EstadoPrenda.EN_PROCESO));
                case FINALIZADO ->
                        cb.and(
                                vigente,
                                cb.not(hayPrenda(root, query, cb, EstadoPrenda.EN_PROCESO)),
                                hayPrenda(root, query, cb, EstadoPrenda.FINALIZADO));
                case ENTREGADO -> cb.and(vigente, cb.not(haySinEntregar(root, query, cb)));
            };
        };
    }

    static Specification<Pedido> sastre(UUID sastreId) {
        if (sastreId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> {
            Subquery<Integer> sub = query.subquery(Integer.class);
            Root<Prenda> p = sub.from(Prenda.class);
            sub.select(cb.literal(1))
                    .where(cb.equal(p.get("pedido"), root), cb.equal(p.get("sastreId"), sastreId));
            return cb.exists(sub);
        };
    }

    /** No anulado, con fecha de entrega ya pasada y alguna prenda sin entregar. */
    static Specification<Pedido> vencidos(LocalDate hoy) {
        return (root, query, cb) ->
                cb.and(
                        cb.isFalse(root.get("anulado")),
                        cb.lessThan(root.get("fechaEntrega"), hoy),
                        haySinEntregar(root, query, cb));
    }

    /** No anulado y con alguna prenda sin entregar (pendiente de entrega). */
    static Specification<Pedido> sinEntregar() {
        return (root, query, cb) ->
                cb.and(cb.isFalse(root.get("anulado")), haySinEntregar(root, query, cb));
    }

    /** No anulado, para entregar ese dia y con alguna prenda sin entregar. */
    static Specification<Pedido> paraEntregarEl(LocalDate dia) {
        return (root, query, cb) ->
                cb.and(
                        cb.isFalse(root.get("anulado")),
                        cb.equal(root.get("fechaEntrega"), dia),
                        haySinEntregar(root, query, cb));
    }

    /** No anulado y con saldo: (suma de prendas − descuento) > abonos vigentes. */
    static Specification<Pedido> conSaldo() {
        return (root, query, cb) -> {
            Subquery<BigDecimal> prendas = query.subquery(BigDecimal.class);
            Root<Prenda> p = prendas.from(Prenda.class);
            prendas.select(
                            cb.coalesce(
                                    cb.sum(
                                            cb.prod(
                                                    p.<BigDecimal>get("precioUnitario"),
                                                    p.<Integer>get("cantidad")
                                                            .as(BigDecimal.class))),
                                    BigDecimal.ZERO))
                    .where(cb.equal(p.get("pedido"), root));

            Subquery<BigDecimal> abonos = query.subquery(BigDecimal.class);
            Root<Abono> a = abonos.from(Abono.class);
            abonos.select(cb.coalesce(cb.sum(a.<BigDecimal>get("monto")), BigDecimal.ZERO))
                    .where(cb.equal(a.get("pedido"), root), cb.isFalse(a.get("anulado")));

            return cb.and(
                    cb.isFalse(root.get("anulado")),
                    cb.greaterThan(cb.diff(prendas, root.<BigDecimal>get("descuento")), abonos));
        };
    }

    private static Specification<Pedido> igual(String campo, Object valor) {
        return valor == null
                ? Specification.unrestricted()
                : (root, query, cb) -> cb.equal(root.get(campo), valor);
    }

    private static Specification<Pedido> entregaDesde(LocalDate desde) {
        return desde == null
                ? Specification.unrestricted()
                : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fechaEntrega"), desde);
    }

    private static Specification<Pedido> entregaHasta(LocalDate hasta) {
        return hasta == null
                ? Specification.unrestricted()
                : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("fechaEntrega"), hasta);
    }

    private static Predicate hayPrenda(
            Root<Pedido> root, CriteriaQuery<?> query, CriteriaBuilder cb, EstadoPrenda estado) {
        Subquery<Integer> sub = query.subquery(Integer.class);
        Root<Prenda> p = sub.from(Prenda.class);
        sub.select(cb.literal(1))
                .where(cb.equal(p.get("pedido"), root), cb.equal(p.get("estado"), estado));
        return cb.exists(sub);
    }

    private static Predicate haySinEntregar(
            Root<Pedido> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Subquery<Integer> sub = query.subquery(Integer.class);
        Root<Prenda> p = sub.from(Prenda.class);
        sub.select(cb.literal(1))
                .where(
                        cb.equal(p.get("pedido"), root),
                        cb.notEqual(p.get("estado"), EstadoPrenda.ENTREGADO));
        return cb.exists(sub);
    }
}
