package com.needlos.empleados;

import com.needlos.common.exception.CodigoError;
import com.needlos.common.exception.ConflictoException;
import com.needlos.common.exception.RecursoNoEncontradoException;
import com.needlos.common.exception.ReglaNegocioException;
import com.needlos.common.tenant.TenantContext;
import com.needlos.empleados.dto.EmpleadoDtos.ActualizarEmpleadoRequest;
import com.needlos.empleados.dto.EmpleadoDtos.CrearEmpleadoRequest;
import com.needlos.empleados.dto.EmpleadoDtos.EmpleadoResponse;
import com.needlos.empleados.dto.InvitacionDtos.InvitacionResponse;
import com.needlos.security.acceso.Acceso;
import com.needlos.security.acceso.AccesoRepository;
import com.needlos.security.cuenta.Cuenta;
import com.needlos.security.cuenta.CuentaRepository;
import com.needlos.security.recuperacion.RecuperacionProperties;
import com.needlos.security.rol.Rol;
import com.needlos.security.rol.RolRepository;
import com.needlos.security.token.TokenAleatorio;
import com.needlos.tenant.TenantRepository;
import com.needlos.tenant.domain.Tenant;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestion del equipo de una sastreria. Invitar NO agrega a la persona: crea una {@link Invitacion}
 * pendiente y envia un correo con un enlace para aceptarla; la cuenta y el acceso se crean al
 * aceptar (ver {@link InvitacionService}). Todo opera sobre la sastreria del usuario autenticado
 * ({@link TenantContext}); el controlador exige rol SASTRE_ADMIN. Reglas de seguridad: no se puede
 * dejar la sastreria sin ningun SASTRE_ADMIN activo; desactivar un acceso es soft.
 */
@Service
@Transactional
public class EmpleadoService {

    private static final String ROL_ADMIN = "SASTRE_ADMIN";
    private static final String RUTA_ACEPTAR = "/aceptar-invitacion#token=";

    private final AccesoRepository accesoRepo;
    private final CuentaRepository cuentaRepo;
    private final RolRepository rolRepo;
    private final TenantRepository tenantRepo;
    private final InvitacionRepository invitacionRepo;
    private final EmpleadoProperties props;
    private final RecuperacionProperties recuperacionProps;
    private final ApplicationEventPublisher eventos;

    public EmpleadoService(
            AccesoRepository accesoRepo,
            CuentaRepository cuentaRepo,
            RolRepository rolRepo,
            TenantRepository tenantRepo,
            InvitacionRepository invitacionRepo,
            EmpleadoProperties props,
            RecuperacionProperties recuperacionProps,
            ApplicationEventPublisher eventos) {
        this.accesoRepo = accesoRepo;
        this.cuentaRepo = cuentaRepo;
        this.rolRepo = rolRepo;
        this.tenantRepo = tenantRepo;
        this.invitacionRepo = invitacionRepo;
        this.props = props;
        this.recuperacionProps = recuperacionProps;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    public List<EmpleadoResponse> listar() {
        UUID tenant = TenantContext.getTenantId();
        List<Acceso> accesos = accesoRepo.findDeTenant(tenant);
        Map<UUID, Cuenta> cuentas =
                cuentaRepo.findAllById(accesos.stream().map(Acceso::getCuentaId).toList()).stream()
                        .collect(Collectors.toMap(Cuenta::getId, Function.identity()));
        return accesos.stream()
                .map(a -> toResponse(a, cuentas.get(a.getCuentaId())))
                .sorted(
                        Comparator.comparing(
                                EmpleadoResponse::nombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InvitacionResponse> listarInvitaciones() {
        return invitacionRepo.findPendientes(TenantContext.getTenantId()).stream()
                .map(EmpleadoService::toInvitacionResponse)
                .toList();
    }

    /** Invita por correo: crea una invitacion pendiente y envia el enlace para aceptarla. */
    public InvitacionResponse invitar(CrearEmpleadoRequest req) {
        UUID tenant = TenantContext.getTenantId();
        Tenant sastreria = sastreriaActual(tenant);
        rol(req.rol()); // valida que el rol exista
        String email = req.email().trim().toLowerCase(Locale.ROOT);

        Cuenta existente = cuentaRepo.findByEmailIgnoreCase(email).orElse(null);
        if (existente != null
                && accesoRepo.existsByCuentaIdAndTenantId(existente.getId(), tenant)) {
            throw new ConflictoException(
                    CodigoError.EMPLEADO_YA_EN_EQUIPO,
                    "Ese correo ya pertenece al equipo de esta sastrería.");
        }

        invitacionRepo.eliminarPendientes(tenant, email);

        String token = TokenAleatorio.nuevo();
        Invitacion invitacion = new Invitacion();
        invitacion.setId(UUID.randomUUID());
        invitacion.setTenantId(tenant);
        invitacion.setEmail(email);
        invitacion.setNombre(req.nombre().trim());
        invitacion.setApellido(req.apellido().trim());
        invitacion.setTelefono(normalizar(req.telefono()));
        invitacion.setNumeroDocumento(normalizar(req.numeroDocumento()));
        invitacion.setRol(req.rol());
        invitacion.setTokenHash(TokenAleatorio.hash(token));
        invitacion.setExpira(Instant.now().plus(Duration.ofHours(props.horasInvitacion())));
        invitacionRepo.save(invitacion);

        publicarInvitacion(invitacion, sastreria.getNombre(), token);
        return toInvitacionResponse(invitacion);
    }

    /** Reenvia una invitacion pendiente: nuevo token, nueva vigencia y correo. */
    public void reenviarInvitacion(UUID invitacionId) {
        UUID tenant = TenantContext.getTenantId();
        Invitacion invitacion = buscarInvitacion(invitacionId, tenant);
        Tenant sastreria = sastreriaActual(tenant);
        String token = TokenAleatorio.nuevo();
        invitacion.setTokenHash(TokenAleatorio.hash(token));
        invitacion.setExpira(Instant.now().plus(Duration.ofHours(props.horasInvitacion())));
        publicarInvitacion(invitacion, sastreria.getNombre(), token);
    }

    public void cancelarInvitacion(UUID invitacionId) {
        UUID tenant = TenantContext.getTenantId();
        invitacionRepo.delete(buscarInvitacion(invitacionId, tenant));
    }

    public EmpleadoResponse actualizar(UUID accesoId, ActualizarEmpleadoRequest req) {
        UUID tenant = TenantContext.getTenantId();
        Acceso acceso = buscarAcceso(accesoId, tenant);
        Rol nuevoRol = rol(req.rol());

        boolean dejariaDeSerAdmin =
                acceso.isActivo() && esAdmin(acceso) && !ROL_ADMIN.equals(req.rol());
        if (dejariaDeSerAdmin && esUltimoAdmin(tenant)) {
            throw ultimoAdmin();
        }

        Cuenta cuenta =
                cuentaRepo
                        .findById(acceso.getCuentaId())
                        .orElseThrow(EmpleadoService::empleadoNoEncontrado);
        cuenta.setNombre(req.nombre().trim());
        cuenta.setApellido(req.apellido().trim());
        cuenta.setTelefono(normalizar(req.telefono()));
        cuenta.setNumeroDocumento(normalizar(req.numeroDocumento()));

        acceso.setRoles(new HashSet<>(Set.of(nuevoRol)));
        // Un SASTRE_ADMIN tiene todos los permisos: no se guardan sueltos.
        acceso.getPermisos().clear();
        if (!ROL_ADMIN.equals(req.rol()) && req.permisos() != null) {
            acceso.getPermisos().addAll(req.permisos());
        }
        return toResponse(acceso, cuenta);
    }

    public void desactivar(UUID accesoId) {
        UUID tenant = TenantContext.getTenantId();
        Acceso acceso = buscarAcceso(accesoId, tenant);
        if (acceso.isActivo() && esAdmin(acceso) && esUltimoAdmin(tenant)) {
            throw ultimoAdmin();
        }
        acceso.setActivo(false);
    }

    public void reactivar(UUID accesoId) {
        UUID tenant = TenantContext.getTenantId();
        buscarAcceso(accesoId, tenant).setActivo(true);
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private void publicarInvitacion(Invitacion invitacion, String sastreria, String token) {
        String enlace = recuperacionProps.urlFrontend() + RUTA_ACEPTAR + token;
        eventos.publishEvent(
                new EmpleadoInvitado(
                        invitacion.getEmail(),
                        invitacion.getNombre(),
                        sastreria,
                        enlace,
                        props.horasInvitacion()));
    }

    private Acceso buscarAcceso(UUID accesoId, UUID tenant) {
        return accesoRepo
                .findDeTenant(accesoId, tenant)
                .orElseThrow(EmpleadoService::empleadoNoEncontrado);
    }

    private Invitacion buscarInvitacion(UUID id, UUID tenant) {
        return invitacionRepo
                .findByIdAndTenantId(id, tenant)
                .filter(i -> i.getUsadoEn() == null)
                .orElseThrow(
                        () ->
                                new RecursoNoEncontradoException(
                                        CodigoError.INVITACION_NO_ENCONTRADA,
                                        "Invitación no encontrada."));
    }

    private Tenant sastreriaActual(UUID tenant) {
        return tenantRepo
                .findById(tenant)
                .orElseThrow(
                        () ->
                                new RecursoNoEncontradoException(
                                        CodigoError.SASTRERIA_NO_ENCONTRADA,
                                        "Sastrería no encontrada."));
    }

    private Rol rol(String nombre) {
        return rolRepo.findByNombre(nombre)
                .orElseThrow(
                        () ->
                                new ReglaNegocioException(
                                        CodigoError.ROL_INVALIDO, "El rol indicado no existe."));
    }

    private boolean esAdmin(Acceso acceso) {
        return acceso.getRoles().stream().anyMatch(r -> ROL_ADMIN.equals(r.getNombre()));
    }

    private boolean esUltimoAdmin(UUID tenant) {
        return accesoRepo.contarAdminsActivos(tenant) <= 1;
    }

    private static String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private EmpleadoResponse toResponse(Acceso acceso, Cuenta cuenta) {
        List<String> roles = acceso.getRoles().stream().map(Rol::getNombre).sorted().toList();
        return new EmpleadoResponse(
                acceso.getId(),
                cuenta.getId(),
                cuenta.getNombre(),
                cuenta.getApellido(),
                cuenta.getEmail(),
                cuenta.getTelefono(),
                cuenta.getNumeroDocumento(),
                roles,
                acceso.getPermisos().stream().map(Enum::name).sorted().toList(),
                acceso.isActivo());
    }

    private static InvitacionResponse toInvitacionResponse(Invitacion i) {
        return new InvitacionResponse(
                i.getId(),
                i.getEmail(),
                i.getNombre(),
                i.getApellido(),
                i.getRol(),
                i.getExpira(),
                i.getCreadoEn());
    }

    private ReglaNegocioException ultimoAdmin() {
        return new ReglaNegocioException(
                CodigoError.ULTIMO_ADMIN,
                "No puedes dejar la sastrería sin ningún administrador activo.");
    }

    private static RecursoNoEncontradoException empleadoNoEncontrado() {
        return new RecursoNoEncontradoException(
                CodigoError.EMPLEADO_NO_ENCONTRADO, "Empleado no encontrado.");
    }
}
