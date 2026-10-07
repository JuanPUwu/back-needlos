package com.needlos.security.auth;

import com.needlos.common.exception.AccesoDenegadoException;
import com.needlos.common.exception.ApiException;
import com.needlos.common.exception.CodigoError;
import com.needlos.common.exception.ConflictoException;
import com.needlos.common.exception.DemasiadasSolicitudesException;
import com.needlos.common.exception.NoAutenticadoException;
import com.needlos.common.exception.ReglaNegocioException;
import com.needlos.common.exception.SolicitudInvalidaException;
import com.needlos.security.acceso.Acceso;
import com.needlos.security.acceso.AccesoRepository;
import com.needlos.security.auth.dto.AuthDtos.CuentaResumen;
import com.needlos.security.auth.dto.AuthDtos.LoginRequest;
import com.needlos.security.auth.dto.AuthDtos.LoginResponse;
import com.needlos.security.auth.dto.AuthDtos.RegistrarSastreriaGoogleRequest;
import com.needlos.security.auth.dto.AuthDtos.RegistrarSastreriaRequest;
import com.needlos.security.auth.dto.AuthDtos.RegistroPendienteResponse;
import com.needlos.security.auth.dto.AuthDtos.SastreriaResumen;
import com.needlos.security.auth.dto.AuthDtos.SeleccionarSastreriaRequest;
import com.needlos.security.auth.dto.AuthDtos.SesionResponse;
import com.needlos.security.cuenta.Cuenta;
import com.needlos.security.cuenta.CuentaBloqueada;
import com.needlos.security.cuenta.CuentaRepository;
import com.needlos.security.cuenta.CuentaVerificada;
import com.needlos.security.google.GoogleTokenVerifier;
import com.needlos.security.google.GoogleTokenVerifier.UsuarioGoogle;
import com.needlos.security.jwt.JwtService;
import com.needlos.security.rol.Rol;
import com.needlos.security.rol.RolRepository;
import com.needlos.security.sesion.ContextoCliente;
import com.needlos.security.sesion.Sesion;
import com.needlos.security.sesion.SesionService;
import com.needlos.security.sesion.SesionService.SesionAbierta;
import com.needlos.security.sesion.SesionService.SesionRenovada;
import com.needlos.security.verificacion.VerificacionCorreoService;
import com.needlos.security.verificacion.VerificacionCorreoSolicitada;
import com.needlos.tenant.GeneradorSlug;
import com.needlos.tenant.TenantRepository;
import com.needlos.tenant.domain.PlanTenant;
import com.needlos.tenant.domain.Tenant;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Flujos de autenticacion: login (correo o Google), eleccion de sastreria, registro de sastreria,
 * renovacion y cierre de sesion.
 *
 * <p>Devuelve el refresh token en claro por separado de la respuesta JSON: el controlador lo coloca
 * en la cookie HttpOnly y nunca en el cuerpo.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final String ROL_DUENO = "SASTRE_ADMIN";
    private static final String ROL_SUPER_ADMIN = "SUPER_ADMIN";
    private static final String MSG_CREDENCIALES = "Correo o contraseña incorrectos.";
    private static final String MSG_SIN_ACCESO =
            "No estás asociado a ninguna sastrería. Pídele a tu jefe (SASTRE_ADMIN) que habilite tu correo.";
    private static final String MSG_CODIGO_INVALIDO =
            "El código no es válido o ya venció. Pide que te enviemos uno nuevo.";

    private final CuentaRepository cuentaRepo;
    private final AccesoRepository accesoRepo;
    private final RolRepository rolRepo;
    private final TenantRepository tenantRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SesionService sesionService;
    private final GoogleTokenVerifier googleVerifier;
    private final GeneradorSlug generadorSlug;
    private final BloqueoCuenta bloqueoCuenta;
    private final VerificacionCorreoService verificacionService;
    private final ApplicationEventPublisher eventos;

    /** Hash de relleno: el login tarda lo mismo exista o no la cuenta (no revela correos). */
    private final String hashSenuelo;

    public AuthService(
            CuentaRepository cuentaRepo,
            AccesoRepository accesoRepo,
            RolRepository rolRepo,
            TenantRepository tenantRepo,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            SesionService sesionService,
            GoogleTokenVerifier googleVerifier,
            GeneradorSlug generadorSlug,
            BloqueoCuenta bloqueoCuenta,
            VerificacionCorreoService verificacionService,
            ApplicationEventPublisher eventos) {
        this.cuentaRepo = cuentaRepo;
        this.accesoRepo = accesoRepo;
        this.rolRepo = rolRepo;
        this.tenantRepo = tenantRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.sesionService = sesionService;
        this.googleVerifier = googleVerifier;
        this.generadorSlug = generadorSlug;
        this.bloqueoCuenta = bloqueoCuenta;
        this.verificacionService = verificacionService;
        this.eventos = eventos;
        this.hashSenuelo = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    /** Respuesta del login + refresh token en claro (null si falta elegir sastreria). */
    public record ResultadoLogin(LoginResponse respuesta, String refreshToken) {}

    /** Sesion emitida + refresh token en claro (null si no hubo rotacion). */
    public record SesionEmitida(SesionResponse respuesta, String refreshToken) {}

    // ── Login ───────────────────────────────────────────────────────

    /**
     * Login con correo y contrasena. Tras varios intentos fallidos seguidos el login con contrasena
     * de ese correo se bloquea unos minutos ({@link BloqueoCuenta}).
     */
    @Transactional(noRollbackFor = ApiException.class)
    public ResultadoLogin login(LoginRequest req, ContextoCliente cliente) {
        String email = normalizar(req.email());
        long bloqueado = bloqueoCuenta.segundosRestantes(email);
        if (bloqueado > 0) {
            throw cuentaBloqueada(bloqueado);
        }

        Cuenta cuenta = cuentaRepo.findByEmailIgnoreCase(email).orElse(null);
        boolean tieneContrasena = cuenta != null && cuenta.getPasswordHash() != null;

        // Siempre se ejecuta BCrypt, incluso si la cuenta no existe.
        boolean coincide =
                passwordEncoder.matches(
                        req.password(), tieneContrasena ? cuenta.getPasswordHash() : hashSenuelo);

        if (!tieneContrasena || !coincide || !cuenta.isActivo()) {
            long bloqueo = bloqueoCuenta.registrarFallo(email);
            if (bloqueo > 0) {
                log.warn(
                        "Login con contrasena bloqueado {} min por intentos fallidos seguidos (ip {})",
                        bloqueoCuenta.minutosDeBloqueo(),
                        cliente.ip());
                if (cuenta != null && cuenta.isActivo()) {
                    // Aviso al dueno de la cuenta (solo si existe; no se avisa por correos
                    // inventados).
                    eventos.publishEvent(
                            new CuentaBloqueada(
                                    cuenta.getEmail(),
                                    cuenta.getNombre(),
                                    bloqueoCuenta.minutosDeBloqueo()));
                }
                throw cuentaBloqueada(bloqueo);
            }
            throw new NoAutenticadoException(CodigoError.CREDENCIALES_INVALIDAS, MSG_CREDENCIALES);
        }
        if (!cuenta.isVerificada()) {
            // La contrasena es correcta (ya se probo), asi que decirselo no revela nada nuevo.
            throw new ReglaNegocioException(
                    CodigoError.CUENTA_SIN_VERIFICAR,
                    "Primero verifica tu correo. Revisa el código que te enviamos al registrarte.");
        }
        bloqueoCuenta.reiniciar(email);
        return resolverSesion(cuenta, cliente);
    }

    /** Login con Google: solo para cuentas ya existentes (no crea sastrerias). */
    @Transactional
    public ResultadoLogin loginGoogle(String idToken, ContextoCliente cliente) {
        UsuarioGoogle google = googleVerifier.verificar(idToken);

        Cuenta cuenta =
                cuentaRepo
                        .findByGoogleSub(google.sub())
                        .or(() -> cuentaRepo.findByEmailIgnoreCase(normalizar(google.email())))
                        .filter(Cuenta::isActivo)
                        .orElseThrow(this::sinAcceso);

        if (cuenta.getGoogleSub() == null) {
            // Primer ingreso con Google de un correo habilitado: se vincula la cuenta.
            cuenta.setGoogleSub(google.sub());
        } else if (!cuenta.getGoogleSub().equals(google.sub())) {
            throw new NoAutenticadoException(
                    CodigoError.CREDENCIALES_INVALIDAS,
                    "Esta cuenta esta vinculada a otra cuenta de Google.");
        }
        if (!cuenta.isVerificada()) {
            // Se habia registrado con correo+contrasena sin confirmar el codigo, pero
            // Google ya probo que el correo es suyo: no hace falta pedirselo de nuevo.
            // La contrasena se BORRA: la puso quien se registro SIN probar ser dueno del
            // correo (podia ser un tercero que lo uso para "reservarlo"); si se conservara,
            // esa persona podria entrar despues a la cuenta del verdadero dueno.
            cuenta.setPasswordHash(null);
            sesionService.cerrarTodas(cuenta.getId());
            cuenta.setVerificada(true);
            eventos.publishEvent(new CuentaVerificada(cuenta.getEmail(), cuenta.getNombre()));
        }
        return resolverSesion(cuenta, cliente);
    }

    /** Elige una sastreria cuando la cuenta tenia varias (usa el preauth token). */
    @Transactional
    public SesionEmitida seleccionarSastreria(
            SeleccionarSastreriaRequest req, ContextoCliente cliente) {
        UUID cuentaId;
        try {
            cuentaId = jwtService.validarPreauth(req.preauthToken());
        } catch (RuntimeException e) {
            throw new NoAutenticadoException(
                    CodigoError.SESION_INVALIDA,
                    "La selección de sastrería expiró. Inicia sesión de nuevo.");
        }

        Cuenta cuenta =
                cuentaRepo
                        .findById(cuentaId)
                        .filter(Cuenta::isActivo)
                        .orElseThrow(
                                () ->
                                        new NoAutenticadoException(
                                                CodigoError.SESION_INVALIDA, MSG_CREDENCIALES));

        Acceso acceso =
                accesoRepo
                        .findVigente(cuentaId, req.tenantId())
                        .orElseThrow(
                                () ->
                                        new AccesoDenegadoException(
                                                CodigoError.SIN_ACCESO_SASTRERIA,
                                                "No tienes acceso a esa sastrería."));

        return emitir(cuenta, acceso, cliente);
    }

    // ── Registro de sastreria ───────────────────────────────────────

    /**
     * Onboarding con correo y contrasena: crea la sastreria (DEMO) y su cuenta dueno
     * (SASTRE_ADMIN), pero SIN sesion todavia: Google verifica el correo solo, pero aqui hay que
     * confirmarlo con el codigo de 6 digitos que se envia por correo ({@link #verificarCorreo})
     * antes de poder entrar.
     */
    @Transactional
    public RegistroPendienteResponse registrarSastreria(
            RegistrarSastreriaRequest req, ContextoCliente cliente) {
        String email = normalizar(req.email());

        Cuenta pendiente =
                cuentaRepo.findByEmailIgnoreCase(email).filter(c -> !c.isVerificada()).orElse(null);
        if (pendiente != null) {
            return repetirRegistroPendiente(pendiente, req);
        }
        validarCorreoDisponible(email);

        Cuenta cuenta = new Cuenta();
        cuenta.setId(UUID.randomUUID());
        cuenta.setEmail(email);
        cuenta.setPasswordHash(passwordEncoder.encode(req.password()));
        cuenta.setNombre(req.nombreAdmin().trim());
        cuenta.setApellido(req.apellidoAdmin().trim());
        cuenta.setNumeroDocumento(req.numeroDocumento().trim());
        cuenta.setTelefono(req.telefono());
        cuenta.setActivo(true);
        cuenta.setVerificada(false);

        crearSastreria(req.nombreSastreria(), cuenta);

        String codigo = verificacionService.generar(cuenta.getId());
        eventos.publishEvent(
                new VerificacionCorreoSolicitada(
                        cuenta.getEmail(),
                        cuenta.getNombre(),
                        codigo,
                        verificacionService.minutosValidez()));

        return new RegistroPendienteResponse(cuenta.getEmail(), null);
    }

    /**
     * El correo ya tenia un registro SIN verificar (lo hizo su dueno y no termino, o un tercero que
     * lo "reservo"). Quien lo repite reemplaza los datos y la contrasena y recibe un codigo nuevo:
     * asi el dueno real nunca queda bloqueado. Para evitar crear sastrerias a repeticion, el nombre
     * de la sastreria solo cambia pasado el tiempo configurado (24 h) desde el primer registro.
     */
    private RegistroPendienteResponse repetirRegistroPendiente(
            Cuenta cuenta, RegistrarSastreriaRequest req) {
        if (verificacionService.limiteReenvioAlcanzado(cuenta.getEmail())) {
            throw new DemasiadasSolicitudesException(
                    CodigoError.DEMASIADAS_SOLICITUDES,
                    "Ya te enviamos varios códigos. Espera un rato antes de pedir otro.",
                    3600);
        }
        Acceso acceso =
                accesoRepo.findVigentes(cuenta.getId()).stream()
                        .findFirst()
                        .orElseThrow(this::sinAcceso);
        Tenant tenant = tenantRepo.findById(acceso.getTenantId()).orElseThrow();

        cuenta.setPasswordHash(passwordEncoder.encode(req.password()));
        cuenta.setNombre(req.nombreAdmin().trim());
        cuenta.setApellido(req.apellidoAdmin().trim());
        cuenta.setNumeroDocumento(req.numeroDocumento().trim());
        cuenta.setTelefono(req.telefono());

        String aviso = null;
        String solicitado = req.nombreSastreria().trim();
        if (!tenant.getNombre().equalsIgnoreCase(solicitado)) {
            Instant desde =
                    cuenta.getCreadoEn()
                            .plus(
                                    verificacionService.horasParaCambiarSastreria(),
                                    ChronoUnit.HOURS);
            if (Instant.now().isAfter(desde)) {
                tenant.setNombre(solicitado);
            } else {
                aviso =
                        "Ya tenías un registro pendiente de la sastrería \""
                                + tenant.getNombre()
                                + "\" con este correo. Te enviamos un código nuevo. Para registrar una sastrería con otro nombre"
                                + " debes esperar "
                                + verificacionService.horasParaCambiarSastreria()
                                + " horas desde ese primer registro.";
            }
        }

        String codigo = verificacionService.generar(cuenta.getId());
        eventos.publishEvent(
                new VerificacionCorreoSolicitada(
                        cuenta.getEmail(),
                        cuenta.getNombre(),
                        codigo,
                        verificacionService.minutosValidez()));
        return new RegistroPendienteResponse(cuenta.getEmail(), aviso);
    }

    /**
     * Onboarding con Google: crea sastreria (DEMO) y su cuenta dueno (SASTRE_ADMIN). Google ya
     * verifico el correo, asi que entra directo (sin codigo) y recibe el correo de bienvenida.
     */
    @Transactional
    public SesionEmitida registrarSastreriaGoogle(
            RegistrarSastreriaGoogleRequest req, ContextoCliente cliente) {
        UsuarioGoogle google = googleVerifier.verificar(req.idToken());
        String email = normalizar(google.email());

        Cuenta pendiente =
                cuentaRepo.findByEmailIgnoreCase(email).filter(c -> !c.isVerificada()).orElse(null);
        if (pendiente != null) {
            return tomarRegistroPendienteConGoogle(pendiente, google, req, cliente);
        }
        validarCorreoDisponible(email);

        Cuenta cuenta = new Cuenta();
        cuenta.setId(UUID.randomUUID());
        cuenta.setEmail(email);
        cuenta.setGoogleSub(google.sub());
        cuenta.setNombre(google.nombre() != null ? google.nombre() : "Dueno");
        cuenta.setApellido(google.apellido() != null ? google.apellido() : "");
        cuenta.setNumeroDocumento(req.numeroDocumento());
        cuenta.setActivo(true);
        cuenta.setVerificada(true);

        Acceso acceso = crearSastreria(req.nombreSastreria(), cuenta);
        eventos.publishEvent(new CuentaVerificada(cuenta.getEmail(), cuenta.getNombre()));
        return emitir(cuenta, acceso, cliente);
    }

    /**
     * Quien se registra con Google sobre un correo que tenia un registro pendiente ya probo ser su
     * dueno: toma la cuenta (se borra la contrasena que puso otra persona), queda verificada y la
     * sastreria toma el nombre que pidio ahora (sin esperar).
     */
    private SesionEmitida tomarRegistroPendienteConGoogle(
            Cuenta cuenta,
            UsuarioGoogle google,
            RegistrarSastreriaGoogleRequest req,
            ContextoCliente cliente) {
        cuentaRepo
                .findByGoogleSub(google.sub())
                .filter(c -> !c.getId().equals(cuenta.getId()))
                .ifPresent(
                        c -> {
                            throw new ConflictoException(
                                    CodigoError.CORREO_EN_USO,
                                    "Esta cuenta de Google ya está asociada a otro correo registrado. Inicia sesión.");
                        });
        Acceso acceso =
                accesoRepo.findVigentes(cuenta.getId()).stream()
                        .findFirst()
                        .orElseThrow(this::sinAcceso);

        cuenta.setGoogleSub(google.sub());
        cuenta.setPasswordHash(null);
        cuenta.setVerificada(true);
        cuenta.setNombre(google.nombre() != null ? google.nombre() : cuenta.getNombre());
        cuenta.setApellido(google.apellido() != null ? google.apellido() : cuenta.getApellido());
        if (req.numeroDocumento() != null) {
            cuenta.setNumeroDocumento(req.numeroDocumento());
        }
        sesionService.cerrarTodas(cuenta.getId());
        tenantRepo
                .findById(acceso.getTenantId())
                .orElseThrow()
                .setNombre(req.nombreSastreria().trim());

        eventos.publishEvent(new CuentaVerificada(cuenta.getEmail(), cuenta.getNombre()));
        return emitir(cuenta, acceso, cliente);
    }

    // ── Verificacion de correo (registro con contrasena) ─────────────

    /**
     * Confirma el codigo de 6 digitos y entra directo con una sesion nueva. Una cuenta YA
     * verificada rechaza la llamada (no da sesion sin validar nada): el codigo es de un solo uso
     * para activar la cuenta, no una forma alterna de iniciar sesion sin contrasena.
     *
     * <p>{@code noRollbackFor}: llama a {@link VerificacionCorreoService#validarYConsumir} en la
     * MISMA transaccion (propagacion REQUIRED); sin esto, el rollback por defecto ante su excepcion
     * esperada deshace el contador de intentos fallidos cada vez (el noRollbackFor del metodo
     * interno no basta: quien manda es el limite exterior de la transaccion).
     */
    @Transactional(noRollbackFor = ApiException.class)
    public SesionEmitida verificarCorreo(
            String emailIngresado, String codigo, ContextoCliente cliente) {
        String email = normalizar(emailIngresado);
        Cuenta cuenta =
                cuentaRepo
                        .findByEmailIgnoreCase(email)
                        .filter(Cuenta::isActivo)
                        .orElseThrow(this::codigoInvalido);

        if (cuenta.isVerificada()) {
            throw new ReglaNegocioException(
                    CodigoError.CUENTA_YA_VERIFICADA,
                    "Esta cuenta ya está verificada. Inicia sesión normalmente.");
        }

        verificacionService.validarYConsumir(cuenta.getId(), codigo);
        cuenta.setVerificada(true);
        eventos.publishEvent(new CuentaVerificada(cuenta.getEmail(), cuenta.getNombre()));

        Acceso acceso =
                accesoRepo.findVigentes(cuenta.getId()).stream()
                        .findFirst()
                        .orElseThrow(this::sinAcceso);
        return emitir(cuenta, acceso, cliente);
    }

    /** Reenvia un codigo nuevo. Responde igual exista o no la cuenta, o ya este verificada. */
    @Transactional
    public void reenviarCodigoVerificacion(String emailIngresado) {
        String email = normalizar(emailIngresado);
        Cuenta cuenta =
                cuentaRepo.findByEmailIgnoreCase(email).filter(Cuenta::isActivo).orElse(null);
        if (cuenta == null
                || cuenta.isVerificada()
                || verificacionService.limiteReenvioAlcanzado(email)) {
            return;
        }
        String codigo = verificacionService.generar(cuenta.getId());
        eventos.publishEvent(
                new VerificacionCorreoSolicitada(
                        cuenta.getEmail(),
                        cuenta.getNombre(),
                        codigo,
                        verificacionService.minutosValidez()));
    }

    // ── Renovacion y cierre ─────────────────────────────────────────

    /**
     * Renueva la sesion con el refresh de la cookie. Vuelve a comprobar que la cuenta y el acceso
     * sigan vigentes; si no, revoca la sesion. No revierte la transaccion ante errores esperados
     * para que las revocaciones queden guardadas.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public SesionEmitida renovar(String refreshToken, ContextoCliente cliente) {
        SesionRenovada renovada = sesionService.renovar(refreshToken, cliente);
        Sesion sesion = renovada.sesion();

        Cuenta cuenta =
                cuentaRepo.findById(sesion.getCuentaId()).filter(Cuenta::isActivo).orElse(null);
        if (cuenta == null) {
            sesion.revocar();
            throw new NoAutenticadoException(
                    CodigoError.SESION_INVALIDA, "Tu cuenta esta desactivada.");
        }

        SesionResponse respuesta;
        if (sesion.getTenantId() == null) {
            if (!cuenta.isSuperAdmin()) {
                sesion.revocar();
                throw new NoAutenticadoException(
                        CodigoError.SESION_INVALIDA, "Tu sesión ya no es válida.");
            }
            respuesta = respuestaSuperAdmin(cuenta, sesion);
        } else {
            Acceso acceso =
                    accesoRepo.findVigente(cuenta.getId(), sesion.getTenantId()).orElse(null);
            if (acceso == null) {
                sesion.revocar();
                throw new AccesoDenegadoException(
                        CodigoError.SIN_ACCESO_SASTRERIA, "Ya no tienes acceso a esa sastrería.");
            }
            respuesta = respuestaSastreria(cuenta, acceso, sesion);
        }
        return new SesionEmitida(respuesta, renovada.refreshToken());
    }

    @Transactional
    public void cerrarSesion(String refreshToken) {
        sesionService.cerrarPorToken(refreshToken);
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private ResultadoLogin resolverSesion(Cuenta cuenta, ContextoCliente cliente) {
        // El SUPER_ADMIN entra sin pertenecer a ninguna sastreria (ve todas).
        if (cuenta.isSuperAdmin()) {
            SesionAbierta abierta = sesionService.abrir(cuenta.getId(), null, cliente);
            SesionResponse sesion = respuestaSuperAdmin(cuenta, abierta.sesion());
            return new ResultadoLogin(
                    new LoginResponse(false, null, null, sesion), abierta.refreshToken());
        }

        List<Acceso> accesos = accesoRepo.findVigentes(cuenta.getId());
        if (accesos.isEmpty()) {
            throw sinAcceso();
        }
        if (accesos.size() == 1) {
            SesionEmitida emitida = emitir(cuenta, accesos.get(0), cliente);
            return new ResultadoLogin(
                    new LoginResponse(false, null, null, emitida.respuesta()),
                    emitida.refreshToken());
        }

        Map<UUID, Tenant> tenants =
                tenantRepo.findAllById(accesos.stream().map(Acceso::getTenantId).toList()).stream()
                        .collect(Collectors.toMap(Tenant::getId, Function.identity()));
        List<SastreriaResumen> sastrerias =
                accesos.stream().map(a -> resumen(a, tenants.get(a.getTenantId()))).toList();
        String preauth = jwtService.generarPreauthToken(cuenta.getId());
        return new ResultadoLogin(new LoginResponse(true, preauth, sastrerias, null), null);
    }

    private SesionEmitida emitir(Cuenta cuenta, Acceso acceso, ContextoCliente cliente) {
        SesionAbierta abierta = sesionService.abrir(cuenta.getId(), acceso.getTenantId(), cliente);
        return new SesionEmitida(
                respuestaSastreria(cuenta, acceso, abierta.sesion()), abierta.refreshToken());
    }

    private SesionResponse respuestaSastreria(Cuenta cuenta, Acceso acceso, Sesion sesion) {
        List<String> roles = nombresDeRoles(acceso);
        Tenant tenant = tenantRepo.findById(acceso.getTenantId()).orElseThrow();
        String token =
                jwtService.generarAccessToken(
                        cuenta.getId(), acceso.getTenantId(), sesion.getId(), roles);
        return new SesionResponse(
                token,
                jwtService.segundosDeVidaAccessToken(),
                roles,
                resumen(cuenta),
                resumen(acceso, tenant));
    }

    private SesionResponse respuestaSuperAdmin(Cuenta cuenta, Sesion sesion) {
        List<String> roles = List.of(ROL_SUPER_ADMIN);
        String token = jwtService.generarAccessToken(cuenta.getId(), null, sesion.getId(), roles);
        return new SesionResponse(
                token, jwtService.segundosDeVidaAccessToken(), roles, resumen(cuenta), null);
    }

    /** Crea la sastreria (DEMO) y el acceso de SASTRE_ADMIN de la cuenta dada. No emite sesion. */
    private Acceso crearSastreria(String nombreSastreria, Cuenta cuenta) {
        String nombre = nombreSastreria.trim();
        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setNombre(nombre);
        tenant.setSlug(generadorSlug.generar(nombre));
        tenant.setPlan(PlanTenant.DEMO);
        tenant.setActivo(true);
        tenantRepo.save(tenant);

        cuentaRepo.save(cuenta);

        Rol rolDueno =
                rolRepo.findByNombre(ROL_DUENO)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Rol " + ROL_DUENO + " no configurado."));

        Acceso acceso = new Acceso();
        acceso.setId(UUID.randomUUID());
        acceso.setCuentaId(cuenta.getId());
        acceso.setTenantId(tenant.getId());
        acceso.setRoles(Set.of(rolDueno));
        acceso.setActivo(true);
        accesoRepo.save(acceso);

        return acceso;
    }

    private void validarCorreoDisponible(String email) {
        if (cuentaRepo.existsByEmailIgnoreCase(email)) {
            throw new ConflictoException(
                    CodigoError.CORREO_EN_USO,
                    "Ya existe una cuenta con ese correo. Inicia sesión.");
        }
    }

    private List<String> nombresDeRoles(Acceso acceso) {
        return acceso.getRoles().stream().map(Rol::getNombre).sorted().toList();
    }

    private SastreriaResumen resumen(Acceso acceso, Tenant tenant) {
        return new SastreriaResumen(
                tenant.getId(), tenant.getNombre(), tenant.getSlug(), nombresDeRoles(acceso));
    }

    private CuentaResumen resumen(Cuenta cuenta) {
        return new CuentaResumen(
                cuenta.getId(), cuenta.getEmail(), cuenta.getNombre(), cuenta.getApellido());
    }

    private DemasiadasSolicitudesException cuentaBloqueada(long segundos) {
        long minutos = Math.max(1, (segundos + 59) / 60);
        return new DemasiadasSolicitudesException(
                CodigoError.CUENTA_BLOQUEADA_TEMPORALMENTE,
                "Por seguridad bloqueamos el inicio de sesión con contraseña de esta cuenta por varios intentos"
                        + " fallidos. Intentalo de nuevo en "
                        + minutos
                        + (minutos == 1 ? " minuto" : " minutos")
                        + " o entra con Google si tu cuenta lo tiene vinculado.",
                segundos);
    }

    private AccesoDenegadoException sinAcceso() {
        return new AccesoDenegadoException(CodigoError.SIN_ACCESO_SASTRERIA, MSG_SIN_ACCESO);
    }

    private SolicitudInvalidaException codigoInvalido() {
        return new SolicitudInvalidaException(
                CodigoError.CODIGO_VERIFICACION_INVALIDO, MSG_CODIGO_INVALIDO);
    }

    private static String normalizar(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
