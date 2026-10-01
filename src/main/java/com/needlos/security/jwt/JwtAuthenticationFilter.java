package com.needlos.security.jwt;

import com.needlos.common.tenant.TenantContext;
import com.needlos.security.sesion.SesionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Autentica cada peticion con el access token del header Authorization:
 *   1. Verifica la firma y vigencia del JWT.
 *   2. Verifica que la sesion (sid) siga activa en BD: un logout o una sesion
 *      cerrada invalida el token al instante, sin esperar a que expire.
 *   3. Establece la Authentication (con los roles) y el {@link TenantContext}.
 * Si algo falla la peticion sigue sin autenticar y Spring Security responde 401.
 * El contexto se limpia siempre al final para no filtrar datos entre peticiones.
 *
 * No es un @Component: se registra solo dentro de la cadena de seguridad.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;
    private final SesionService sesionService;

    public JwtAuthenticationFilter(JwtService jwtService, SesionService sesionService) {
        this.jwtService = jwtService;
        this.sesionService = sesionService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(PREFIJO)) {
            try {
                UsuarioAutenticado usuario = jwtService.validar(header.substring(PREFIJO.length()));
                if (sesionService.estaActiva(usuario.sesionId(), usuario.cuentaId())) {
                    var authorities = usuario.roles().stream()
                            .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol))
                            .toList();
                    var auth = new UsernamePasswordAuthenticationToken(usuario, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    TenantContext.set(usuario.tenantId(), usuario.cuentaId());
                }
            } catch (RuntimeException ex) {
                // Token invalido, manipulado o expirado: la peticion sigue sin autenticar.
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
