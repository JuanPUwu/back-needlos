package com.needlos.security.config;

import com.needlos.common.web.CorrelationIdFilter;
import com.needlos.security.filtro.LimitadorSolicitudes;
import com.needlos.security.filtro.OrigenPermitidoFilter;
import com.needlos.security.filtro.RateLimitFilter;
import com.needlos.security.filtro.RateLimitProperties;
import com.needlos.security.jwt.JwtAuthenticationFilter;
import com.needlos.security.jwt.JwtProperties;
import com.needlos.security.jwt.JwtService;
import com.needlos.security.recuperacion.RecuperacionProperties;
import com.needlos.security.verificacion.VerificacionProperties;
import com.needlos.security.sesion.SesionProperties;
import com.needlos.security.sesion.SesionService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;

/**
 * Seguridad de la API:
 *  · Sin estado en el servidor web: cada peticion trae su access token (Bearer).
 *  · Solo los endpoints de autenticacion listados son publicos; todo lo demas exige sesion.
 *  · CSRF: la unica cookie es la del refresh (HttpOnly, SameSite=Lax, Path=/api/v1/auth);
 *    sus endpoints ademas verifican el Origin ({@link OrigenPermitidoFilter}). El token CSRF
 *    clasico de Spring no aplica porque el resto de la API no usa cookies.
 *  · Errores 401/403/429 en el formato estandar (ProblemDetail + code).
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({JwtProperties.class, SesionProperties.class, RateLimitProperties.class,
        RecuperacionProperties.class, VerificacionProperties.class})
public class SecurityConfig {

    /** Endpoints publicos (todos POST). */
    private static final String[] AUTH_PUBLICOS = {
            "/api/v1/auth/login",
            "/api/v1/auth/google",
            "/api/v1/auth/seleccionar-sastreria",
            "/api/v1/auth/registrar-sastreria",
            "/api/v1/auth/registrar-sastreria-google",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout",
            "/api/v1/auth/recuperar-contrasena",
            "/api/v1/auth/restablecer-contrasena",
            "/api/v1/auth/verificar-correo",
            "/api/v1/auth/reenviar-codigo"
    };

    /** Solo responden si springdoc esta activo (perfil dev). */
    private static final String[] SWAGGER = {"/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**"};

    private final List<String> origenesPermitidos;
    private final String csp;
    private final HandlerExceptionResolver resolver;

    public SecurityConfig(@Value("${needlos.cors.allowed-origins}") String allowedOrigins,
                          @Value("${needlos.seguridad.csp:}") String csp,
                          @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.origenesPermitidos = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(o -> !o.isEmpty())
                .toList();
        this.csp = csp;
        this.resolver = resolver;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtService jwtService,
                                           SesionService sesionService,
                                           LimitadorSolicitudes limitador,
                                           RateLimitProperties rateLimit) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // Sin .configurationSource(...) aqui a proposito: si se pasa explicita, Spring
                // Security ignora el bean "corsFilter" de abajo (y su formato de error propio).
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> {
                    headers.frameOptions(frame -> frame.deny());
                    headers.referrerPolicy(ref -> ref.policy(ReferrerPolicy.NO_REFERRER));
                    headers.httpStrictTransportSecurity(hsts -> hsts
                            .includeSubDomains(true)
                            .maxAgeInSeconds(31_536_000));
                    if (!csp.isBlank()) {
                        headers.contentSecurityPolicy(c -> c.policyDirectives(csp));
                    }
                })
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> resolver.resolveException(req, res, null, e))
                        .accessDeniedHandler((req, res, e) -> resolver.resolveException(req, res, null, e)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, AUTH_PUBLICOS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers(SWAGGER).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new OrigenPermitidoFilter(origenesPermitidos, resolver),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new RateLimitFilter(limitador, rateLimit, resolver),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, sesionService),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origenesPermitidos);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE,
                CorrelationIdFilter.CABECERA, "Idempotency-Key"));
        config.setExposedHeaders(List.of(CorrelationIdFilter.CABECERA, HttpHeaders.RETRY_AFTER));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Spring Security busca un bean llamado exactamente "corsFilter" y, si existe, lo usa
     * en vez de construir uno por defecto (asi se respeta el {@code .cors(...)} de arriba,
     * pero con nuestro formato de error al rechazar un origen no permitido).
     */
    @Bean
    public CorsFilter corsFilter(CorsConfigurationSource corsConfigurationSource, ObjectMapper objectMapper) {
        CorsFilter filter = new CorsFilter(corsConfigurationSource);
        filter.setCorsProcessor(new OrigenNoPermitidoCorsProcessor(objectMapper));
        return filter;
    }
}
