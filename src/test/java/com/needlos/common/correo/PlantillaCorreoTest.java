package com.needlos.common.correo;

import static org.assertj.core.api.Assertions.assertThat;

import com.needlos.common.correo.PlantillaCorreo.Codigo;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlantillaCorreoTest {

    @Test
    void elCodigoVaSinEspaciosNiSeparadores_parapoderCopiarloYPegarlo() {
        String html =
                PlantillaCorreo.htmlConCodigo(
                        "Verifica tu correo", List.of("Hola"), new Codigo("123456"));

        assertThat(html).contains(">123456</div>");
        assertThat(html).doesNotContain("123 456").doesNotContain("123&nbsp;456");
    }

    @Test
    void escapaElContenidoDelUsuario() {
        String html = PlantillaCorreo.html("T", List.of("<script>alert(1)</script>"), null);

        assertThat(html).doesNotContain("<script>").contains("&lt;script&gt;");
    }
}
