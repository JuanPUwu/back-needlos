package com.needlos.tenant;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class GeneradorSlugTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Sastrería Ñandú & Hijos   | sastreria-nandu-hijos",
            "  El Buen Corte  | el-buen-corte",
            "Confecciones #1 (Centro) | confecciones-1-centro",
            "AB               | sastreria-ab",
            "¡¡¡              | sastreria"
    })
    void normaliza_elNombreAUnIdentificadorLegible(String nombre, String esperado) {
        assertThat(GeneradorSlug.normalizar(nombre)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @CsvSource({"50", "120"})
    void normaliza_recortaNombresLargosSinGuionFinal(int largo) {
        String slug = GeneradorSlug.normalizar("a".repeat(largo - 1) + " b");

        assertThat(slug).hasSizeLessThanOrEqualTo(50).doesNotEndWith("-");
    }
}
