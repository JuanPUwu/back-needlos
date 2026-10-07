package com.needlos.security.auth;

import com.needlos.security.sesion.SesionProperties;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Cookie del refresh token: HttpOnly (JavaScript no puede leerla), Secure (solo HTTPS, salvo en
 * dev), SameSite=Lax y limitada a las rutas de /api/v1/auth.
 */
@Component
public class CookieRefresh {

    public static final String NOMBRE = "needlos_refresh";
    private static final String RUTA = "/api/v1/auth";

    private final SesionProperties props;

    public CookieRefresh(SesionProperties props) {
        this.props = props;
    }

    public ResponseCookie crear(String refreshToken) {
        return base(refreshToken).maxAge(Duration.ofDays(props.duracionDias())).build();
    }

    public ResponseCookie borrar() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String valor) {
        return ResponseCookie.from(NOMBRE, valor)
                .httpOnly(true)
                .secure(props.cookieSegura())
                .sameSite("Lax")
                .path(RUTA);
    }
}
