package com.needlos.tipoprenda;

import com.needlos.common.exception.CodigoError;
import com.needlos.common.exception.ConflictoException;
import com.needlos.common.exception.RecursoNoEncontradoException;
import com.needlos.common.tenant.TenantContext;
import com.needlos.common.web.IfMatch;
import com.needlos.tipoprenda.domain.TipoPrenda;
import com.needlos.tipoprenda.dto.TipoPrendaDtos.GuardarTipoPrendaRequest;
import com.needlos.tipoprenda.dto.TipoPrendaDtos.OrigenPrecio;
import com.needlos.tipoprenda.dto.TipoPrendaDtos.PrecioSugeridoResponse;
import com.needlos.tipoprenda.dto.TipoPrendaDtos.TipoPrendaResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Catalogo de tipos de prenda de la sastreria. Nombre unico por sastreria (sin distinguir
 * mayusculas). No se borran: se desactivan, porque los pedidos los referencian; uno inactivo no se
 * ofrece en pedidos nuevos pero sigue en los existentes.
 */
@Service
@Transactional
public class TipoPrendaService {

    private final TipoPrendaRepository repo;

    public TipoPrendaService(TipoPrendaRepository repo) {
        this.repo = repo;
    }

    /** Activos (para elegir en un pedido) o todos (para administrar el catalogo). */
    @Transactional(readOnly = true)
    public List<TipoPrendaResponse> listar(boolean incluirInactivos) {
        List<TipoPrenda> tipos =
                incluirInactivos
                        ? repo.findAllByOrderByNombreAsc()
                        : repo.findByActivoTrueOrderByNombreAsc();
        return tipos.stream().map(TipoPrendaService::toResponse).toList();
    }

    public TipoPrendaResponse crear(GuardarTipoPrendaRequest req) {
        String nombre = req.nombre().trim();
        if (repo.existsByNombreIgnoreCase(nombre)) {
            throw duplicado(nombre);
        }
        TipoPrenda tipo = new TipoPrenda();
        tipo.setNombre(nombre);
        tipo.setPrecioBase(req.precioBase());
        tipo.setActivo(true);
        return toResponse(repo.saveAndFlush(tipo));
    }

    public TipoPrendaResponse actualizar(UUID id, long version, GuardarTipoPrendaRequest req) {
        TipoPrenda tipo = buscar(id);
        IfMatch.verificar(version, tipo.getVersion());
        String nombre = req.nombre().trim();
        if (repo.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw duplicado(nombre);
        }
        tipo.setNombre(nombre);
        tipo.setPrecioBase(req.precioBase());
        return toResponse(repo.saveAndFlush(tipo));
    }

    public TipoPrendaResponse desactivar(UUID id) {
        TipoPrenda tipo = buscar(id);
        tipo.setActivo(false);
        return toResponse(repo.saveAndFlush(tipo));
    }

    public TipoPrendaResponse reactivar(UUID id) {
        TipoPrenda tipo = buscar(id);
        tipo.setActivo(true);
        return toResponse(repo.saveAndFlush(tipo));
    }

    /** Ultimo precio cobrado (sin pedidos anulados); si no hay, el precio base; si no, ninguno. */
    @Transactional(readOnly = true)
    public PrecioSugeridoResponse precioSugerido(UUID id) {
        TipoPrenda tipo = buscar(id);
        return repo.ultimoPrecio(TenantContext.getTenantId(), id)
                .map(precio -> new PrecioSugeridoResponse(precio, OrigenPrecio.HISTORICO))
                .orElseGet(
                        () ->
                                tipo.getPrecioBase() != null
                                        ? new PrecioSugeridoResponse(
                                                tipo.getPrecioBase(), OrigenPrecio.PRECIO_BASE)
                                        : new PrecioSugeridoResponse(null, OrigenPrecio.NINGUNO));
    }

    private TipoPrenda buscar(UUID id) {
        return repo.findById(id)
                .orElseThrow(
                        () ->
                                new RecursoNoEncontradoException(
                                        CodigoError.TIPO_PRENDA_NO_ENCONTRADO,
                                        "Tipo de prenda no encontrado."));
    }

    private static ConflictoException duplicado(String nombre) {
        return new ConflictoException(
                CodigoError.TIPO_PRENDA_DUPLICADO,
                "Ya existe un tipo de prenda llamado \"" + nombre + "\" en el catálogo.");
    }

    private static TipoPrendaResponse toResponse(TipoPrenda t) {
        return new TipoPrendaResponse(
                t.getId(), t.getNombre(), t.getPrecioBase(), t.isActivo(), t.getVersion());
    }
}
