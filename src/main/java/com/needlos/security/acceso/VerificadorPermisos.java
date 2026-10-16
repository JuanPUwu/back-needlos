package com.needlos.security.acceso;

import com.needlos.security.jwt.UsuarioAutenticado;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifica los permisos del usuario actual. Se usa en los controladores con
 * {@code @PreAuthorize("@permisos.tiene('CREAR_PEDIDOS')")} y desde los servicios para permisos que
 * dependen de los datos (p. ej. asignar sastre al crear un pedido).
 *
 * <p>Se consulta en la base en cada peticion (no viaja en el JWT): si el SASTRE_ADMIN quita un
 * permiso, aplica de inmediato, sin esperar a que caduque el token.
 */
@Component("permisos")
@Transactional(readOnly = true)
public class VerificadorPermisos {

    static final String ROL_ADMIN = "SASTRE_ADMIN";
    static final String ROL_SASTRE = "SASTRE";

    private final AccesoRepository accesoRepo;

    public VerificadorPermisos(AccesoRepository accesoRepo) {
        this.accesoRepo = accesoRepo;
    }

    /** Para SpEL: nombre del permiso ({@link Permiso}). */
    public boolean tiene(String permiso) {
        return tiene(Permiso.valueOf(permiso));
    }

    public boolean tiene(Permiso permiso) {
        Optional<UsuarioAutenticado> usuario = usuarioActual();
        if (usuario.isEmpty() || usuario.get().tenantId() == null) {
            return false;
        }
        UsuarioAutenticado u = usuario.get();
        if (u.roles().contains(ROL_ADMIN)) {
            return true;
        }
        return u.roles().contains(ROL_SASTRE)
                && accesoRepo.tienePermiso(u.cuentaId(), u.tenantId(), permiso);
    }

    public boolean esAdmin() {
        return usuarioActual().map(u -> u.roles().contains(ROL_ADMIN)).orElse(false);
    }

    static Optional<UsuarioAutenticado> usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof UsuarioAutenticado u
                ? Optional.of(u)
                : Optional.empty();
    }
}
