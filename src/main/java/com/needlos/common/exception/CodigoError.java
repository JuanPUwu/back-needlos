package com.needlos.common.exception;

/**
 * Catalogo unico de codigos de error de la API (campo "code" del ProblemDetail). El frontend decide
 * comportamientos por este codigo, nunca por el texto. Un codigo publicado no se renombra: el
 * cliente movil futuro dependera de el.
 */
public enum CodigoError {

    // ── 400 · Solicitud invalida ────────────────────────────────────
    VALIDACION,
    SOLICITUD_INVALIDA,
    ORDENAMIENTO_NO_PERMITIDO,
    ENLACE_RECUPERACION_INVALIDO,
    CODIGO_VERIFICACION_INVALIDO,

    // ── 401 · No autenticado ────────────────────────────────────────
    NO_AUTENTICADO,
    CREDENCIALES_INVALIDAS,
    SESION_INVALIDA,

    // ── 403 · Sin permiso ───────────────────────────────────────────
    SIN_PERMISO,
    SIN_ACCESO_SASTRERIA,
    ORIGEN_NO_PERMITIDO,

    // ── 404 · No existe (o pertenece a otra sastreria) ──────────────
    RECURSO_NO_ENCONTRADO,
    CLIENTE_NO_ENCONTRADO,
    PEDIDO_NO_ENCONTRADO,
    PRENDA_NO_ENCONTRADA,
    TIPO_PRENDA_NO_ENCONTRADO,
    SESION_NO_ENCONTRADA,

    // ── 405 / 415 ───────────────────────────────────────────────────
    METODO_NO_PERMITIDO,
    FORMATO_NO_SOPORTADO,

    // ── 409 · Conflicto ─────────────────────────────────────────────
    CONFLICTO_CONCURRENCIA,
    CONFLICTO_DATOS,
    CORREO_EN_USO,

    // ── 422 · Regla de negocio incumplida ───────────────────────────
    PEDIDO_ANULADO,
    PEDIDO_YA_ANULADO,
    PRENDA_ENTREGADA,
    CUENTA_SIN_CONTRASENA,
    CONTRASENA_ACTUAL_INCORRECTA,
    CUENTA_SIN_VERIFICAR,
    CUENTA_YA_VERIFICADA,

    // ── 429 / 500 ───────────────────────────────────────────────────
    DEMASIADAS_SOLICITUDES,
    CUENTA_BLOQUEADA_TEMPORALMENTE,
    ERROR_INTERNO
}
