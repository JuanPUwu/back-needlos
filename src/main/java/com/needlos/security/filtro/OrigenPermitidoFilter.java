package com.needlos.security.filtro;

import com.needlos.common.exception.AccesoDenegadoException;
import com.needlos.common.exception.CodigoError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Proteccion CSRF de los endpoints que usan la cookie del refresh (Reglas §8.1).
 * Ademas de SameSite=Lax, exige que la cabecera Origin sea uno de los origenes
 * permitidos: otra web no puede renovar ni cerrar la sesion del usuario.
 */
public class OrigenPermitidoFilter extends OncePerRequestFilter {

    private static final Set<String> RUTAS_CON_COOKIE = Set.of("/api/v1/auth/refresh", "/api/v1/auth/logout");

    private final Set<String> origenesPermitidos;
    private final HandlerExceptionResolver resolver;

    public OrigenPermitidoFilter(List<String> origenesPermitidos, HandlerExceptionResolver resolver) {
        this.origenesPermitidos = Set.copyOf(origenesPermitidos);
        this.resolver = resolver;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !RUTAS_CON_COOKIE.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String origen = request.getHeader(HttpHeaders.ORIGIN);
        if (origen == null || !origenesPermitidos.contains(origen)) {
            resolver.resolveException(request, response, null, new AccesoDenegadoException(
                    CodigoError.ORIGEN_NO_PERMITIDO, "Origen de la solicitud no permitido."));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
