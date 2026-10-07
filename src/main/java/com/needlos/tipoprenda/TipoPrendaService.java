package com.needlos.tipoprenda;

import com.needlos.tipoprenda.dto.TipoPrendaResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
