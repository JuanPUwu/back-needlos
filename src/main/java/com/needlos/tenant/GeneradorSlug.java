package com.needlos.tenant;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Genera el identificador interno (slug) de una sastreria a partir de su nombre. El usuario solo
 * escribe el nombre; el slug no se pide ni se edita.
 *
 * <p>"Sastrería Ñandú & Hijos" -> "sastreria-nandu-hijos"
 *
 * <p>Si ya existe, agrega un sufijo aleatorio corto. La restriccion UNIQUE de la BD cubre el caso
 * improbable de dos registros simultaneos con el mismo nombre.
 */
@Component
public class GeneradorSlug {

    private static final int MAX_BASE = 50;
    private static final int MIN_BASE = 3;
    private static final int MAX_INTENTOS = 5;

    private final TenantRepository tenantRepo;

    public GeneradorSlug(TenantRepository tenantRepo) {
        this.tenantRepo = tenantRepo;
    }

    public String generar(String nombreSastreria) {
        String base = normalizar(nombreSastreria);
        if (!tenantRepo.existsBySlug(base)) {
            return base;
        }
        for (int i = 0; i < MAX_INTENTOS; i++) {
            String candidato = base + "-" + sufijo(4);
            if (!tenantRepo.existsBySlug(candidato)) {
                return candidato;
            }
        }
        return base + "-" + sufijo(8);
    }

    static String normalizar(String nombre) {
        String sinTildes =
                Normalizer.normalize(nombre, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug =
                sinTildes
                        .toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9]+", "-")
                        .replaceAll("(^-+|-+$)", "");
        if (slug.length() > MAX_BASE) {
            slug = slug.substring(0, MAX_BASE).replaceAll("-+$", "");
        }
        if (slug.length() < MIN_BASE) {
            slug = slug.isEmpty() ? "sastreria" : "sastreria-" + slug;
        }
        return slug;
    }

    private static String sufijo(int largo) {
        return UUID.randomUUID().toString().replace("-", "").substring(0, largo);
    }
}
