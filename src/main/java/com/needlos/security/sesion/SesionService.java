package com.needlos.security.sesion;

import com.needlos.common.exception.ApiException;
import com.needlos.common.exception.CodigoError;
import com.needlos.common.exception.NoAutenticadoException;
import com.needlos.common.exception.RecursoNoEncontradoException;
import com.needlos.security.token.TokenAleatorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Ciclo de vida de las sesiones: abrir, rotar el refresh token, validar y revocar.
 *
 * Seguridad:
 *  · El refresh token es aleatorio (256 bits) y solo se guarda su hash SHA-256.
 *  · Cada renovacion emite un refresh nuevo (rotacion).
 *  · Si se presenta un refresh ya rotado fuera de la ventana de gracia, se asume
 *    robo y se revocan TODAS las sesiones de la cuenta (deteccion de reuso).
 */
@Service
public class SesionService {

    private static final Logger log = LoggerFactory.getLogger(SesionService.class);
    private static final String MSG_SESION_INVALIDA = "Tu sesion expiro. Inicia sesion de nuevo.";

    private final SesionRepository repo;
    private final SesionProperties props;

    public SesionService(SesionRepository repo, SesionProperties props) {
        this.repo = repo;
        this.props = props;
    }

    /** Sesion recien abierta junto al refresh token en claro (solo para la cookie). */
    public record SesionAbierta(Sesion sesion, String refreshToken) {
    }

    /** Resultado de renovar: refreshToken es null si no hubo rotacion (ventana de gracia). */
    public record SesionRenovada(Sesion sesion, String refreshToken) {
    }

    @Transactional
    public SesionAbierta abrir(UUID cuentaId, UUID tenantId, ContextoCliente cliente) {
        String refresh = TokenAleatorio.nuevo();
        Instant ahora = Instant.now();

        Sesion sesion = new Sesion();
        sesion.setId(UUID.randomUUID());
        sesion.setCuentaId(cuentaId);
        sesion.setTenantId(tenantId);
        sesion.setTokenHash(TokenAleatorio.hash(refresh));
        sesion.setDeviceInfo(cliente.dispositivo());
        sesion.setIpAddress(cliente.ip());
        sesion.setUserAgent(cliente.userAgent());
        sesion.setCreadoEn(ahora);
        sesion.setUltimoUso(ahora);
        sesion.setExpira(ahora.plus(Duration.ofDays(props.duracionDias())));
        repo.save(sesion);

        return new SesionAbierta(sesion, refresh);
    }

    /**
     * Renueva la sesion a partir del refresh token de la cookie. No revierte la
     * transaccion ante errores esperados: la revocacion por reuso debe quedar guardada.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public SesionRenovada renovar(String refreshToken, ContextoCliente cliente) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw sesionInvalida();
        }
        String hashActual = TokenAleatorio.hash(refreshToken);

        Sesion sesion = repo.findByTokenHash(hashActual).orElse(null);
        if (sesion == null) {
            return renovarConTokenAnterior(hashActual);
        }
        if (!sesion.estaActiva()) {
            throw sesionInvalida();
        }

        String nuevo = TokenAleatorio.nuevo();
        Instant ahora = Instant.now();
        sesion.setTokenHashAnterior(hashActual);
        sesion.setTokenHash(TokenAleatorio.hash(nuevo));
        sesion.setRotadaEn(ahora);
        sesion.setUltimoUso(ahora);
        sesion.setExpira(ahora.plus(Duration.ofDays(props.duracionDias())));
        sesion.setIpAddress(cliente.ip());
        sesion.setUserAgent(cliente.userAgent());
        sesion.setDeviceInfo(cliente.dispositivo());
        return new SesionRenovada(sesion, nuevo);
    }

    private SesionRenovada renovarConTokenAnterior(String hashPresentado) {
        Sesion sesion = repo.findByTokenHashAnterior(hashPresentado).orElseThrow(this::sesionInvalida);
        if (!sesion.estaActiva()) {
            // Sesion ya cerrada o vencida: no hay nada que proteger.
            throw sesionInvalida();
        }

        boolean dentroDeGracia = sesion.getRotadaEn() != null
                && sesion.getRotadaEn().plusSeconds(props.graciaRotacionSegundos()).isAfter(Instant.now());
        if (dentroDeGracia) {
            // Otra pestana ya roto el token; el navegador ya tiene la cookie nueva.
            return new SesionRenovada(sesion, null);
        }

        int revocadas = repo.revocarTodas(sesion.getCuentaId());
        log.warn("Reuso de refresh token detectado en la sesion {} de la cuenta {}. Sesiones revocadas: {}",
                sesion.getId(), sesion.getCuentaId(), revocadas);
        throw sesionInvalida();
    }

    /** Revoca la sesion dueña del refresh token (logout). No falla si no existe. */
    @Transactional
    public void cerrarPorToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        String h = TokenAleatorio.hash(refreshToken);
        repo.findByTokenHash(h)
                .or(() -> repo.findByTokenHashAnterior(h))
                .ifPresent(Sesion::revocar);
    }

    @Transactional(readOnly = true)
    public boolean estaActiva(UUID sesionId, UUID cuentaId) {
        return repo.findById(sesionId)
                .filter(s -> s.getCuentaId().equals(cuentaId))
                .map(Sesion::estaActiva)
                .orElse(false);
    }

    /** Sesiones abiertas de la cuenta; marca como "actual" la de la peticion en curso. */
    @Transactional(readOnly = true)
    public List<SesionActivaResponse> activas(UUID cuentaId, UUID sesionActual) {
        return repo.findActivas(cuentaId, Instant.now()).stream()
                .map(s -> new SesionActivaResponse(s.getId(), s.getDeviceInfo(), s.getIpAddress(),
                        s.getCreadoEn(), s.getUltimoUso(), s.getId().equals(sesionActual)))
                .toList();
    }

    /** Cierra una sesion propia. Una sesion de otra cuenta responde 404 (no se revela). */
    @Transactional
    public void cerrar(UUID cuentaId, UUID sesionId) {
        Sesion sesion = repo.findById(sesionId)
                .filter(s -> s.getCuentaId().equals(cuentaId))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        CodigoError.SESION_NO_ENCONTRADA, "La sesion no existe."));
        sesion.revocar();
    }

    @Transactional
    public void cerrarOtras(UUID cuentaId, UUID sesionActual) {
        repo.revocarOtras(cuentaId, sesionActual);
    }

    @Transactional
    public void cerrarTodas(UUID cuentaId) {
        repo.revocarTodas(cuentaId);
    }

    private NoAutenticadoException sesionInvalida() {
        return new NoAutenticadoException(CodigoError.SESION_INVALIDA, MSG_SESION_INVALIDA);
    }
}
