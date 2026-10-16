package com.needlos.empleados;

import com.needlos.common.exception.CodigoError;
import com.needlos.common.exception.SolicitudInvalidaException;
import com.needlos.empleados.dto.InvitacionDtos.InvitacionInfo;
import com.needlos.security.acceso.Acceso;
import com.needlos.security.acceso.AccesoRepository;
import com.needlos.security.cuenta.Cuenta;
import com.needlos.security.cuenta.CuentaRepository;
import com.needlos.security.rol.Rol;
import com.needlos.security.rol.RolRepository;
import com.needlos.security.token.TokenAleatorio;
import com.needlos.tenant.TenantRepository;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aceptacion de invitaciones (endpoints publicos, solo con el token del enlace). Al aceptar se crea
 * la cuenta (si no existe) y el acceso a la sastreria; la invitacion es de un solo uso.
 */
@Service
@Transactional
public class InvitacionService {

    private static final String MSG_INVALIDA =
            "La invitación no es válida o ya venció. Pídele a tu jefe que te envíe una nueva.";

    private final InvitacionRepository invitacionRepo;
    private final CuentaRepository cuentaRepo;
    private final AccesoRepository accesoRepo;
    private final RolRepository rolRepo;
    private final TenantRepository tenantRepo;
    private final PasswordEncoder passwordEncoder;

    public InvitacionService(
            InvitacionRepository invitacionRepo,
            CuentaRepository cuentaRepo,
            AccesoRepository accesoRepo,
            RolRepository rolRepo,
            TenantRepository tenantRepo,
            PasswordEncoder passwordEncoder) {
        this.invitacionRepo = invitacionRepo;
        this.cuentaRepo = cuentaRepo;
        this.accesoRepo = accesoRepo;
        this.rolRepo = rolRepo;
        this.tenantRepo = tenantRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public InvitacionInfo info(String token) {
        Invitacion invitacion = vigente(token);
        String sastreria =
                tenantRepo
                        .findById(invitacion.getTenantId())
                        .map(t -> t.getNombre())
                        .orElse("tu sastrería");
        Cuenta cuenta = cuentaRepo.findByEmailIgnoreCase(invitacion.getEmail()).orElse(null);
        return new InvitacionInfo(
                sastreria,
                invitacion.getEmail(),
                invitacion.getNombre(),
                requiereContrasena(cuenta));
    }

    /** Acepta la invitacion: crea la cuenta (si no existe) y el acceso a la sastreria. */
    public void aceptar(String token, String contrasena) {
        Invitacion invitacion = vigente(token);
        Cuenta cuenta = cuentaRepo.findByEmailIgnoreCase(invitacion.getEmail()).orElse(null);
        boolean requiere = requiereContrasena(cuenta);

        if (requiere && (contrasena == null || contrasena.isBlank())) {
            throw new SolicitudInvalidaException(
                    CodigoError.VALIDACION, "Debes definir una contraseña para aceptar.");
        }

        if (cuenta == null) {
            cuenta = new Cuenta();
            cuenta.setId(UUID.randomUUID());
            cuenta.setEmail(invitacion.getEmail());
            cuenta.setNombre(invitacion.getNombre());
            cuenta.setApellido(invitacion.getApellido());
            cuenta.setTelefono(invitacion.getTelefono());
            cuenta.setNumeroDocumento(invitacion.getNumeroDocumento());
            cuenta.setActivo(true);
            cuenta.setVerificada(true);
            cuenta.setPasswordHash(passwordEncoder.encode(contrasena));
            cuenta = cuentaRepo.save(cuenta);
        } else if (requiere) {
            // Cuenta sin forma de entrar (p. ej. quedo a medias): define su contrasena.
            cuenta.setPasswordHash(passwordEncoder.encode(contrasena));
        }

        if (!accesoRepo.existsByCuentaIdAndTenantId(cuenta.getId(), invitacion.getTenantId())) {
            Rol rol = rolRepo.findByNombre(invitacion.getRol()).orElseThrow(this::invalida);
            Acceso acceso = new Acceso();
            acceso.setId(UUID.randomUUID());
            acceso.setCuentaId(cuenta.getId());
            acceso.setTenantId(invitacion.getTenantId());
            acceso.setActivo(true);
            acceso.setRoles(new HashSet<>(Set.of(rol)));
            accesoRepo.save(acceso);
        }

        invitacion.marcarUsada();
        invitacionRepo.eliminarPendientes(invitacion.getTenantId(), invitacion.getEmail());
    }

    private Invitacion vigente(String token) {
        return invitacionRepo
                .findByTokenHash(TokenAleatorio.hash(token))
                .filter(Invitacion::vigente)
                .orElseThrow(this::invalida);
    }

    private boolean requiereContrasena(Cuenta cuenta) {
        return cuenta == null
                || (cuenta.getPasswordHash() == null && cuenta.getGoogleSub() == null);
    }

    private SolicitudInvalidaException invalida() {
        return new SolicitudInvalidaException(CodigoError.INVITACION_INVALIDA, MSG_INVALIDA);
    }
}
