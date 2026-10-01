package com.needlos.security.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.needlos.security.filtro.RateLimitProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Bloqueo temporal del login con contrasena por correo (Reglas §8.1). Complementa
 * el limite por IP: frena ataques repartidos entre muchas IP contra un mismo correo.
 *
 *  · La clave es el correo normalizado, exista o no la cuenta: el comportamiento es
 *    identico y no revela que correos estan registrados.
 *  · N fallos seguidos (sin pasar la ventana entre uno y otro) bloquean el login con
 *    contrasena durante la ventana; al vencer se desbloquea solo.
 *  · Intentar durante el bloqueo no lo alarga. Un login exitoso o restablecer la
 *    contrasena por correo reinician el contador.
 *  · Solo afecta a la contrasena: el dueno legitimo puede seguir entrando con Google.
 *
 * Vive en memoria: vale para una sola instancia del backend (Manual-Fases F11).
 */
@Component
public class BloqueoCuenta {

    private final int maxFallos;
    private final Duration duracion;
    private final Cache<String, Estado> estados;

    public BloqueoCuenta(RateLimitProperties props) {
        this.maxFallos = props.intentosFallidosPorCuenta();
        this.duracion = Duration.ofMinutes(props.bloqueoCuentaMinutos());
        this.estados = Caffeine.newBuilder()
                .expireAfterWrite(duracion)
                .maximumSize(100_000)
                .build();
    }

    /** Segundos que faltan para desbloquear; 0 si el correo no esta bloqueado. */
    public long segundosRestantes(String email) {
        Estado estado = estados.getIfPresent(email);
        if (estado == null || estado.bloqueadoHasta() == null) {
            return 0;
        }
        long milis = estado.bloqueadoHasta().toEpochMilli() - System.currentTimeMillis();
        return milis > 0 ? (milis + 999) / 1000 : 0;
    }

    /** Registra un fallo. Devuelve los segundos de bloqueo si este fallo lo activo; 0 si no. */
    public long registrarFallo(String email) {
        Estado estado = estados.asMap().compute(email, (clave, actual) -> {
            int fallos = (actual == null ? 0 : actual.fallos()) + 1;
            return fallos >= maxFallos
                    ? new Estado(0, Instant.now().plus(duracion))
                    : new Estado(fallos, null);
        });
        return estado.bloqueadoHasta() != null ? duracion.toSeconds() : 0;
    }

    public void reiniciar(String email) {
        estados.invalidate(email);
    }

    public int minutosDeBloqueo() {
        return (int) duracion.toMinutes();
    }

    private record Estado(int fallos, Instant bloqueadoHasta) {
    }
}
