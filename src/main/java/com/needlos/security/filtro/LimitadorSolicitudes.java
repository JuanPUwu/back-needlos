package com.needlos.security.filtro;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Contador de solicitudes por clave (p. ej. "login:203.0.113.5") con ventana fija.
 * Vive en memoria: vale para una sola instancia del backend. Si algun dia hay
 * varias instancias, debe pasar a un almacen compartido (Manual-Fases F11).
 */
@Component
public class LimitadorSolicitudes {

    private final Cache<String, Ventana> ventanas = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(100_000)
            .build();

    /**
     * Registra una solicitud.
     *
     * @return 0 si se permite; si no, los segundos que faltan para poder reintentar
     */
    public long consumir(String clave, int maximo, Duration duracion) {
        long ahora = System.currentTimeMillis();
        Ventana ventana = ventanas.asMap().compute(clave, (k, actual) ->
                actual == null || actual.finMillis() <= ahora
                        ? new Ventana(ahora + duracion.toMillis(), 1)
                        : new Ventana(actual.finMillis(), actual.cuenta() + 1));
        if (ventana.cuenta() <= maximo) {
            return 0;
        }
        return Math.max(1, (ventana.finMillis() - ahora + 999) / 1000);
    }

    private record Ventana(long finMillis, int cuenta) {
    }
}
