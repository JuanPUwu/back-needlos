package com.needlos.empleados;

import com.needlos.common.tenant.TenantContext;
import com.needlos.empleados.dto.EmpleadoDtos.MiembroAsignableResponse;
import com.needlos.security.acceso.Acceso;
import com.needlos.security.acceso.AccesoRepository;
import com.needlos.security.cuenta.Cuenta;
import com.needlos.security.cuenta.CuentaRepository;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consultas de solo lectura sobre el equipo que usan otros modulos (p. ej. pedidos): quien se puede
 * asignar a una prenda y los nombres para mostrar. Siempre dentro de la sastreria actual.
 */
@Service
@Transactional(readOnly = true)
public class DirectorioEquipo {

    private final AccesoRepository accesoRepo;
    private final CuentaRepository cuentaRepo;

    public DirectorioEquipo(AccesoRepository accesoRepo, CuentaRepository cuentaRepo) {
        this.accesoRepo = accesoRepo;
        this.cuentaRepo = cuentaRepo;
    }

    /** Miembros activos de la sastreria, por nombre. */
    public List<MiembroAsignableResponse> asignables() {
        List<Acceso> accesos = accesoRepo.findByTenantIdAndActivoTrue(TenantContext.getTenantId());
        Map<UUID, String> nombres =
                nombresDeCuentas(accesos.stream().map(Acceso::getCuentaId).toList());
        return accesos.stream()
                .map(a -> new MiembroAsignableResponse(a.getId(), nombres.get(a.getCuentaId())))
                .sorted(
                        Comparator.comparing(
                                MiembroAsignableResponse::nombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** Si el acceso es un miembro activo de la sastreria actual. */
    public boolean esMiembroActivo(UUID accesoId) {
        return accesoRepo
                .findDeTenant(accesoId, TenantContext.getTenantId())
                .map(Acceso::isActivo)
                .orElse(false);
    }

    /** Nombre completo de cada acceso de la sastreria actual (incluye los desactivados). */
    public Map<UUID, String> nombresDeAccesos(Collection<UUID> accesoIds) {
        Set<UUID> ids = Set.copyOf(accesoIds);
        if (ids.isEmpty()) {
            return Map.of();
        }
        UUID tenant = TenantContext.getTenantId();
        List<Acceso> accesos =
                accesoRepo.findAllById(ids).stream()
                        .filter(a -> a.getTenantId().equals(tenant))
                        .toList();
        Map<UUID, String> cuentas =
                nombresDeCuentas(accesos.stream().map(Acceso::getCuentaId).toList());
        return accesos.stream()
                .collect(Collectors.toMap(Acceso::getId, a -> cuentas.get(a.getCuentaId())));
    }

    /** Nombre completo de cada cuenta (quien registro un cambio o un abono). */
    public Map<UUID, String> nombresDeCuentas(Collection<UUID> cuentaIds) {
        Set<UUID> ids = Set.copyOf(cuentaIds);
        if (ids.isEmpty()) {
            return Map.of();
        }
        return cuentaRepo.findAllById(ids).stream()
                .collect(
                        Collectors.toMap(
                                Cuenta::getId, c -> c.getNombre() + " " + c.getApellido()));
    }
}
