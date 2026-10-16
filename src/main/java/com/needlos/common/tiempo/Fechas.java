package com.needlos.common.tiempo;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Fechas de negocio en la hora de Colombia. La base y la JVM trabajan en UTC; "hoy" para una
 * sastreria es el dia en America/Bogota (a las 8 p. m. en Bogota ya es manana en UTC).
 */
public final class Fechas {

    public static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private Fechas() {}

    public static LocalDate hoy() {
        return LocalDate.now(ZONA);
    }
}
