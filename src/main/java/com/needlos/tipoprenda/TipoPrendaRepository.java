package com.needlos.tipoprenda;

import com.needlos.tipoprenda.domain.TipoPrenda;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TipoPrendaRepository extends JpaRepository<TipoPrenda, UUID> {
    List<TipoPrenda> findByActivoTrueOrderByNombreAsc();
}
