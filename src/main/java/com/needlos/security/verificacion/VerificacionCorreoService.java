package com.needlos.security.verificacion;

import com.needlos.common.exception.ApiException;
import com.needlos.common.exception.CodigoError;
import com.needlos.common.exception.SolicitudInvalidaException;
import com.needlos.security.filtro.LimitadorSolicitudes;
import com.needlos.security.token.TokenAleatorio;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mecanica del codigo de verificacion de correo (6 digitos): · Un codigo de 6 digitos SI es
 * adivinable por fuerza bruta (1 millon de combinaciones), a diferencia de los tokens largos de
 * recuperacion/refresh. Por eso: expira pronto, sirve una vez, y se invalida tras pocos intentos
 * fallidos (hay que pedir uno nuevo). · Cada codigo nuevo reemplaza al anterior: solo el ultimo
 * sirve. · El reenvio esta limitado por hora y por correo (Caffeine, igual que la recuperacion de
 * contrasena).
 */
@Service
public class VerificacionCorreoService {

    private static final String MSG_CODIGO_INVALIDO =
            "El código no es válido o ya venció. Pide que te enviemos uno nuevo.";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final VerificacionCorreoRepository repo;
    private final LimitadorSolicitudes limitador;
    private final VerificacionProperties props;

    public VerificacionCorreoService(
            VerificacionCorreoRepository repo,
            LimitadorSolicitudes limitador,
            VerificacionProperties props) {
        this.repo = repo;
        this.limitador = limitador;
        this.props = props;
    }

    public int minutosValidez() {
        return props.minutosValidez();
    }

    public int horasParaCambiarSastreria() {
        return props.horasParaCambiarSastreria();
    }

    /**
     * Genera un codigo nuevo para la cuenta (invalida cualquier anterior) y lo devuelve en claro.
     */
    @Transactional
    public String generar(UUID cuentaId) {
        repo.eliminarDeCuenta(cuentaId);

        String codigo = "%06d".formatted(RANDOM.nextInt(1_000_000));
        VerificacionCorreo verificacion = new VerificacionCorreo();
        verificacion.setId(UUID.randomUUID());
        verificacion.setCuentaId(cuentaId);
        verificacion.setCodigoHash(TokenAleatorio.hash(codigo));
        verificacion.setExpira(Instant.now().plus(Duration.ofMinutes(props.minutosValidez())));
        repo.save(verificacion);
        return codigo;
    }

    /** true si ya se alcanzo el limite de reenvios de esta hora para ese correo. */
    public boolean limiteReenvioAlcanzado(String email) {
        return limitador.consumir(
                        "verificar-correo:" + email,
                        props.solicitudesPorHora(),
                        Duration.ofHours(1))
                > 0;
    }

    /**
     * Valida el codigo ingresado y, si es correcto, consume el registro (una sola vez). Un codigo
     * incorrecto cuenta como intento fallido; agotados los intentos, el codigo se invalida y hay
     * que pedir uno nuevo (no sigue aceptando intentos).
     *
     * <p>{@code noRollbackFor}: el metodo lanza una excepcion esperada en el camino de error, pero
     * el intento fallido (o el borrado al agotarlos) debe quedar guardado igual; sin esto, el
     * rollback automatico de Spring deshace el contador cada vez.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public void validarYConsumir(UUID cuentaId, String codigoIngresado) {
        VerificacionCorreo verificacion =
                repo.findFirstByCuentaIdOrderByCreadoEnDesc(cuentaId)
                        .orElseThrow(this::codigoInvalido);

        if (!verificacion.vigente()) {
            repo.delete(verificacion);
            throw codigoInvalido();
        }
        if (!verificacion.getCodigoHash().equals(TokenAleatorio.hash(codigoIngresado))) {
            verificacion.setIntentosFallidos(verificacion.getIntentosFallidos() + 1);
            if (verificacion.getIntentosFallidos() >= props.intentosMaximos()) {
                repo.delete(verificacion);
            } else {
                repo.save(verificacion);
            }
            throw codigoInvalido();
        }
        repo.delete(verificacion);
    }

    private SolicitudInvalidaException codigoInvalido() {
        return new SolicitudInvalidaException(
                CodigoError.CODIGO_VERIFICACION_INVALIDO, MSG_CODIGO_INVALIDO);
    }
}
