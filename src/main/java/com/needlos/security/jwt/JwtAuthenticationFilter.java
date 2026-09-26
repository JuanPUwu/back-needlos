package com.needlos.security.jwt;

import com.needlos.common.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Se ejecuta una vez por request. Si hay un access token valido en el header
 * Authorization:
 *   1. Establece la Authentication de Spring Security (con los roles).
 *   2. Rellena el {@link TenantContext} con tenant y usuario, para que
 *      Hibernate aisle por tenant y la auditoria sepa quien actua.
 * El contexto se limpia siempre al final (finally) para no filtrar datos entre
 * requests que reutilizan el mismo hilo.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                JwtService.DatosToken datos = jwtService.validar(token);

                List<SimpleGrantedAuthority> authorities = datos.roles().stream()
                        .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol))
                        .toList();

                var auth = new UsernamePasswordAuthenticationToken(
                        datos.usuarioId(), null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);

                TenantContext.set(datos.tenantId(), datos.usuarioId());
            } catch (Exception ex) {
                // Token invalido o expirado: seguimos sin autenticar; Spring
                // Security bloqueara el acceso a los endpoints protegidos (401).
                SecurityContextHolder.clearContext();
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
