package com.needlos.clientes;

import com.needlos.clientes.domain.Cliente;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros del listado de clientes. Cada filtro es opcional: si viene vacio no restringe nada. Se
 * combinan con AND.
 */
final class ClienteFiltros {

    private ClienteFiltros() {}

    static Specification<Cliente> de(String buscar, LocalDate desde, LocalDate hasta) {
        return Specification.allOf(texto(buscar), registradoDesde(desde), registradoHasta(hasta));
    }

    /** Nombre, apellido o telefono que contengan el texto (sin distinguir mayusculas). */
    private static Specification<Cliente> texto(String buscar) {
        if (buscar == null || buscar.isBlank()) {
            return Specification.unrestricted();
        }
        String patron = "%" + buscar.trim().toLowerCase() + "%";
        return (root, query, cb) ->
                cb.or(
                        cb.like(cb.lower(root.get("nombre")), patron),
                        cb.like(cb.lower(root.get("apellido")), patron),
                        cb.like(root.get("telefono"), patron));
    }

    private static Specification<Cliente> registradoDesde(LocalDate desde) {
        return desde == null
                ? Specification.unrestricted()
                : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fechaRegistro"), desde);
    }

    private static Specification<Cliente> registradoHasta(LocalDate hasta) {
        return hasta == null
                ? Specification.unrestricted()
                : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("fechaRegistro"), hasta);
    }
}
