package com.needlos.tenant;

import com.needlos.common.exception.CodigoError;
import com.needlos.common.exception.RecursoNoEncontradoException;
import com.needlos.common.tenant.TenantContext;
import com.needlos.tenant.domain.Tenant;
import com.needlos.tenant.dto.SastreriaDtos.ActualizarSastreriaRequest;
import com.needlos.tenant.dto.SastreriaDtos.SastreriaResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Configuracion de la sastreria del usuario autenticado ({@link TenantContext}). El controlador ya
 * exige rol SASTRE_ADMIN. El slug y el plan no se editan aqui (el slug es interno; el plan lo
 * cambia el SUPER_ADMIN con una licencia).
 */
@Service
@Transactional
public class SastreriaService {

    private final TenantRepository repo;

    public SastreriaService(TenantRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public SastreriaResponse obtener() {
        return toResponse(actual());
    }

    public SastreriaResponse actualizar(ActualizarSastreriaRequest req) {
        Tenant tenant = actual();
        tenant.setNombre(req.nombre().trim());
        tenant.setNit(normalizar(req.nit()));
        tenant.setDireccion(normalizar(req.direccion()));
        tenant.setTelefono(normalizar(req.telefono()));
        return toResponse(tenant);
    }

    private Tenant actual() {
        return repo.findById(TenantContext.getTenantId())
                .orElseThrow(
                        () ->
                                new RecursoNoEncontradoException(
                                        CodigoError.SASTRERIA_NO_ENCONTRADA,
                                        "Sastrería no encontrada."));
    }

    private static String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private SastreriaResponse toResponse(Tenant t) {
        return new SastreriaResponse(
                t.getId(),
                t.getNombre(),
                t.getNit(),
                t.getDireccion(),
                t.getTelefono(),
                t.getPlan().name(),
                t.getLicenciaHasta(),
                t.isActivo());
    }
}
