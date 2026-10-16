package com.needlos.security.acceso;

import com.needlos.common.exception.CodigoError;
import com.needlos.common.exception.RecursoNoEncontradoException;
import com.needlos.security.jwt.UsuarioAutenticado;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Permisos del usuario actual en la sastreria activa (para mostrar u ocultar acciones). */
@Service
@Transactional(readOnly = true)
public class PermisoService {

    private final AccesoRepository accesoRepo;

    public PermisoService(AccesoRepository accesoRepo) {
        this.accesoRepo = accesoRepo;
    }

    public MisPermisosResponse mios() {
        UsuarioAutenticado u =
                VerificadorPermisos.usuarioActual()
                        .filter(usuario -> usuario.tenantId() != null)
                        .orElseThrow(PermisoService::sinAcceso);
        Acceso acceso =
                accesoRepo
                        .findVigente(u.cuentaId(), u.tenantId())
                        .orElseThrow(PermisoService::sinAcceso);
        boolean admin = u.roles().contains(VerificadorPermisos.ROL_ADMIN);
        List<String> permisos =
                (admin ? Arrays.stream(Permiso.values()) : acceso.getPermisos().stream())
                        .map(Enum::name)
                        .sorted()
                        .toList();
        return new MisPermisosResponse(acceso.getId(), admin, permisos);
    }

    /** Acceso del usuario actual en la sastreria activa (su id como sastre en las prendas). */
    public UUID miAccesoId() {
        return mios().accesoId();
    }

    private static RecursoNoEncontradoException sinAcceso() {
        return new RecursoNoEncontradoException(
                CodigoError.EMPLEADO_NO_ENCONTRADO, "No tienes acceso a esta sastrería.");
    }

    /**
     * @param accesoId el acceso del usuario en esta sastreria (es su id como sastre en las prendas)
     * @param admin SASTRE_ADMIN: tiene todos los permisos
     */
    public record MisPermisosResponse(UUID accesoId, boolean admin, List<String> permisos) {}
}
