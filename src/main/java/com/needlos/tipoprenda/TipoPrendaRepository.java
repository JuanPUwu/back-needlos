package com.needlos.tipoprenda;

import com.needlos.tipoprenda.domain.TipoPrenda;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoPrendaRepository extends JpaRepository<TipoPrenda, UUID> {
    List<TipoPrenda> findByActivoTrueOrderByNombreAsc();
}
