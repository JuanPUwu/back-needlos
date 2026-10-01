package com.needlos.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Asigna un id de correlacion a cada peticion (Reglas §13). Se guarda en el MDC
 * para que aparezca en todos los logs de la peticion y se devuelve en la
 * cabecera X-Request-Id, de modo que un error reportado por un usuario se
 * pueda rastrear. Si el cliente envia un id valido, se reutiliza.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CABECERA = "X-Request-Id";
    public static final String MDC_CLAVE = "correlationId";

    private static final Pattern FORMATO_VALIDO = Pattern.compile("^[A-Za-z0-9-]{8,64}$");

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String recibido = request.getHeader(CABECERA);
        String correlationId = recibido != null && FORMATO_VALIDO.matcher(recibido).matches()
                ? recibido
                : UUID.randomUUID().toString();

        MDC.put(MDC_CLAVE, correlationId);
        response.setHeader(CABECERA, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_CLAVE);
        }
    }
}
