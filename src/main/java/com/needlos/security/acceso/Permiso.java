package com.needlos.security.acceso;

/**
 * Acciones que el SASTRE_ADMIN otorga a cada miembro de su equipo (decision del dueno 2026-10-15).
 * Un SASTRE nuevo no tiene ninguno; el SASTRE_ADMIN los tiene todos siempre. Ver pedidos y clientes
 * no requiere permiso. Lo exclusivo del SASTRE_ADMIN (editar y anular pedidos, anular abonos,
 * eliminar clientes, catalogo, equipo) no es un permiso asignable.
 */
public enum Permiso {
    /** Crear y editar clientes desde la seccion Clientes. */
    GESTIONAR_CLIENTES,
    /** Crear pedidos (incluye crear al cliente nuevo desde el pedido y poner el precio). */
    CREAR_PEDIDOS,
    /** Pasar prendas entre EN_PROCESO y FINALIZADO. */
    CAMBIAR_ESTADO_PRENDAS,
    /** Marcar prendas como ENTREGADAS. */
    ENTREGAR_PRENDAS,
    /** Asignar o cambiar el sastre de una prenda. */
    ASIGNAR_SASTRE,
    /** Registrar abonos (incluido el abono inicial al crear el pedido). */
    REGISTRAR_ABONOS
}
