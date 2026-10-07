package com.needlos.security.recuperacion;

import com.needlos.common.exception.CodigoError;
import com.needlos.common.exception.SolicitudInvalidaException;
import com.needlos.security.auth.BloqueoCuenta;
import com.needlos.security.cuenta.ContrasenaCambiada;
import com.needlos.security.cuenta.Cuenta;
import com.needlos.security.cuenta.CuentaRepository;
import com.needlos.security.filtro.LimitadorSolicitudes;
import com.needlos.security.sesion.ContextoCliente;
import com.needlos.security.sesion.SesionService;
import com.needlos.security.token.TokenAleatorio;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recuperacion de contrasena por correo (OWASP Forgot Password): · La solicitud responde igual
 * exista o no el correo, y el correo se envia en segundo plano despues del commit (el tiempo de
 * respuesta no revela nada). · Enlace de un solo uso, con vencimiento corto; solo se guarda el hash
 * del token. · Maximo de solicitudes por correo y por hora (evita inundar la bandeja). · Una cuenta
 * sin contrasena (solo Google) recibe un correo que le indica entrar con Google: la recuperacion no
 * crea contrasenas nuevas. · Al restablecer: se cierran todas las sesiones y se levanta el bloqueo
 * por intentos.
 */
@Service
public class RecuperacionContrasenaService {

    private static final Logger log = LoggerFactory.getLogger(RecuperacionContrasenaService.class);
    private static final String RUTA_FRONTEND = "/restablecer-contrasena#token=";
    private static final String MSG_ENLACE_INVALIDO =
            "El enlace para restablecer la contraseña no es válido o ya venció. Solicita uno nuevo.";

    private final CuentaRepository cuentaRepo;
    private final RecuperacionContrasenaRepository repo;
    private final PasswordEncoder passwordEncoder;
    private final SesionService sesionService;
    private final BloqueoCuenta bloqueoCuenta;
    private final LimitadorSolicitudes limitador;
    private final RecuperacionProperties props;
    private final ApplicationEventPublisher eventos;

    public RecuperacionContrasenaService(
            CuentaRepository cuentaRepo,
            RecuperacionContrasenaRepository repo,
            PasswordEncoder passwordEncoder,
            SesionService sesionService,
            BloqueoCuenta bloqueoCuenta,
            LimitadorSolicitudes limitador,
            RecuperacionProperties props,
            ApplicationEventPublisher eventos) {
        this.cuentaRepo = cuentaRepo;
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
        this.sesionService = sesionService;
        this.bloqueoCuenta = bloqueoCuenta;
        this.limitador = limitador;
        this.props = props;
        this.eventos = eventos;
    }

    /** Registra la solicitud y programa el correo. Nunca informa si el correo existe. */
    @Transactional
    public void solicitar(String emailIngresado, ContextoCliente cliente) {
        String email = emailIngresado.trim().toLowerCase(Locale.ROOT);
        if (limitador.consumir(
                        "recuperar:" + email, props.solicitudesPorHora(), Duration.ofHours(1))
                > 0) {
            log.info("Solicitud de recuperacion ignorada: limite por correo alcanzado");
            return;
        }

        Cuenta cuenta =
                cuentaRepo.findByEmailIgnoreCase(email).filter(Cuenta::isActivo).orElse(null);
        if (cuenta == null) {
            return;
        }
        if (cuenta.getPasswordHash() == null) {
            eventos.publishEvent(
                    new RecuperacionSolicitada(cuenta.getEmail(), cuenta.getNombre(), null, 0));
            return;
        }

        repo.eliminarPendientes(cuenta.getId());
        String token = TokenAleatorio.nuevo();
        RecuperacionContrasena recuperacion = new RecuperacionContrasena();
        recuperacion.setId(UUID.randomUUID());
        recuperacion.setCuentaId(cuenta.getId());
        recuperacion.setTokenHash(TokenAleatorio.hash(token));
        recuperacion.setExpira(Instant.now().plus(Duration.ofMinutes(props.minutosValidez())));
        recuperacion.setIpAddress(cliente.ip());
        repo.save(recuperacion);

        eventos.publishEvent(
                new RecuperacionSolicitada(
                        cuenta.getEmail(),
                        cuenta.getNombre(),
                        props.urlFrontend() + RUTA_FRONTEND + token,
                        props.minutosValidez()));
    }

    /** Cambia la contrasena con un enlace vigente y cierra todas las sesiones de la cuenta. */
    @Transactional
    public void restablecer(String token, String contrasenaNueva) {
        RecuperacionContrasena recuperacion =
                repo.findByTokenHash(TokenAleatorio.hash(token))
                        .filter(RecuperacionContrasena::vigente)
                        .orElseThrow(this::enlaceInvalido);
        Cuenta cuenta =
                cuentaRepo
                        .findById(recuperacion.getCuentaId())
                        .filter(Cuenta::isActivo)
                        .orElseThrow(this::enlaceInvalido);

        cuenta.setPasswordHash(passwordEncoder.encode(contrasenaNueva));
        recuperacion.marcarUsada();
        repo.saveAndFlush(recuperacion);
        repo.eliminarPendientes(cuenta.getId());
        sesionService.cerrarTodas(cuenta.getId());
        bloqueoCuenta.reiniciar(cuenta.getEmail());
        eventos.publishEvent(new ContrasenaCambiada(cuenta.getEmail(), cuenta.getNombre()));
        log.info("Contrasena restablecida con enlace de recuperacion (cuenta {})", cuenta.getId());
    }

    private SolicitudInvalidaException enlaceInvalido() {
        return new SolicitudInvalidaException(
                CodigoError.ENLACE_RECUPERACION_INVALIDO, MSG_ENLACE_INVALIDO);
    }
}
