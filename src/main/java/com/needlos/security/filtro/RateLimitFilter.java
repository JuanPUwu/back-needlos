package com.needlos.security.filtro;

import com.needlos.common.exception.DemasiadasSolicitudesException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Limita por IP los intentos contra los endpoints publicos de autenticacion (fuerza bruta, abuso
 * del registro). Superado el limite responde 429 con Retry-After, en el formato de error estandar.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Duration MINUTO = Duration.ofMinutes(1);
    private static final String BASE = "/api/v1/auth/";

    private static final Set<String> RUTAS_AUTH =
            Set.of(
                    "login",
                    "google",
                    "seleccionar-sastreria",
                    "registrar-sastreria",
                    "registrar-sastreria-google",
                    "recuperar-contrasena",
                    "restablecer-contrasena",
                    "verificar-correo",
                    "reenviar-codigo");
    private static final String RUTA_REFRESH = "refresh";

    private final LimitadorSolicitudes limitador;
    private final Map<String, Integer> limites;
    private final HandlerExceptionResolver resolver;

    public RateLimitFilter(
            LimitadorSolicitudes limitador,
            RateLimitProperties props,
            HandlerExceptionResolver resolver) {
        this.limitador = limitador;
        this.resolver = resolver;
        this.limites = Map.of("auth", props.authPorMinuto(), "refresh", props.refreshPorMinuto());
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || grupo(request) == null;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String grupo = grupo(request);
        long esperar =
                limitador.consumir(
                        grupo + ":" + request.getRemoteAddr(), limites.get(grupo), MINUTO);
        if (esperar > 0) {
            resolver.resolveException(
                    request, response, null, new DemasiadasSolicitudesException(esperar));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String grupo(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (!uri.startsWith(BASE)) {
            return null;
        }
        String accion = uri.substring(BASE.length());
        if (RUTA_REFRESH.equals(accion)) {
            return "refresh";
        }
        return RUTAS_AUTH.contains(accion) ? "auth" : null;
    }
}
