package com.needlos.common.consecutivo;

import com.needlos.common.tenant.TenantContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Entrega el siguiente numero consecutivo de la sastreria actual (Reglas §10.4).
 *
 * <p>Usa un contador por sastreria y tipo con un UPSERT atomico: PostgreSQL bloquea la fila del
 * contador hasta el fin de la transaccion, asi que dos pedidos simultaneos nunca reciben el mismo
 * numero (a diferencia de max + 1). Si la transaccion que lo pidio falla, el incremento se revierte
 * con ella.
 */
@Component
public class GeneradorConsecutivo {

    private static final String SQL_SIGUIENTE =
            """
            INSERT INTO consecutivos (tenant_id, tipo, ultimo) VALUES (?, ?, 1)
            ON CONFLICT (tenant_id, tipo) DO UPDATE SET ultimo = consecutivos.ultimo + 1
            RETURNING ultimo
            """;

    private final JdbcTemplate jdbc;

    public GeneradorConsecutivo(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Debe llamarse dentro de la transaccion que crea el registro numerado. */
    @Transactional(propagation = Propagation.MANDATORY)
    public long siguiente(TipoConsecutivo tipo) {
        Long numero =
                jdbc.queryForObject(
                        SQL_SIGUIENTE, Long.class, TenantContext.getTenantId(), tipo.name());
        if (numero == null) {
            throw new IllegalStateException("El contador de consecutivos no devolvio valor.");
        }
        return numero;
    }

    /**
     * Siguiente numero de la serie GLOBAL (compartida por todas las sastrerias). Una secuencia no
     * se revierte con la transaccion: si la creacion falla, ese numero se pierde (hueco aceptado
     * por el dueno, 2026-10-15). A cambio no hay bloqueo entre sastrerias.
     */
    public long siguienteGlobal(TipoConsecutivo tipo) {
        Long numero =
                jdbc.queryForObject("SELECT nextval('" + tipo.secuenciaGlobal() + "')", Long.class);
        if (numero == null) {
            throw new IllegalStateException("La secuencia global no devolvio valor.");
        }
        return numero;
    }
}
