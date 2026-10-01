package com.needlos.tipoprenda;

import com.needlos.tipoprenda.dto.TipoPrendaResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TipoPrendaService {

    private final TipoPrendaRepository repo;

    public TipoPrendaService(TipoPrendaRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<TipoPrendaResponse> listarActivos() {
        return repo.findByActivoTrueOrderByNombreAsc().stream()
                .map(t -> new TipoPrendaResponse(t.getId(), t.getNombre(), t.getPrecioBase()))
                .toList();
    }
}
