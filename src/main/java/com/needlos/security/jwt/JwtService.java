package com.needlos.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Genera y valida los JWT (HS256): · Access token (15 min): sub=cuenta, sid=sesion,
 * tenant_id=sastreria (ausente en SUPER_ADMIN) y roles. Se valida ademas contra la sesion en BD. ·
 * Preauth token (5 min): cuenta autenticada que aun debe elegir sastreria.
 *
 * <p>Si la clave tiene menos de 256 bits la aplicacion no arranca (WeakKeyException).
 */
@Service
public class JwtService {

    private static final String CLAIM_TENANT = "tenant_id";
    private static final String CLAIM_SESION = "sid";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_PREAUTH = "preauth";
    private static final long PREAUTH_MINUTES = 5;

    private final JwtProperties props;
    private final SecretKey key;

    public JwtService(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generarAccessToken(
            UUID cuentaId, UUID tenantId, UUID sesionId, List<String> roles) {
        Instant ahora = Instant.now();
        var builder =
                Jwts.builder()
                        .issuer(props.issuer())
                        .audience()
                        .add(props.audience())
                        .and()
                        .subject(cuentaId.toString())
                        .claim(CLAIM_SESION, sesionId.toString())
                        .claim(CLAIM_ROLES, roles)
                        .issuedAt(Date.from(ahora))
                        .expiration(
                                Date.from(
                                        ahora.plus(
                                                props.accessTokenMinutes(), ChronoUnit.MINUTES)));
        if (tenantId != null) {
            builder.claim(CLAIM_TENANT, tenantId.toString());
        }
        return builder.signWith(key).compact();
    }

    /** Token temporal: cuenta autenticada, pendiente de elegir sastreria. */
    public String generarPreauthToken(UUID cuentaId) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .issuer(props.issuer())
                .audience()
                .add(props.audience())
                .and()
                .subject(cuentaId.toString())
                .claim(CLAIM_PREAUTH, true)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(PREAUTH_MINUTES, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public long segundosDeVidaAccessToken() {
        return props.accessTokenMinutes() * 60;
    }

    /** Valida firma, emisor, audiencia y expiracion de un access token. */
    public UsuarioAutenticado validar(String token) {
        Claims claims = parse(token);
        if (Boolean.TRUE.equals(claims.get(CLAIM_PREAUTH, Boolean.class))) {
            throw new IllegalArgumentException("Un preauth token no sirve como access token.");
        }
        String sid = claims.get(CLAIM_SESION, String.class);
        if (sid == null) {
            throw new IllegalArgumentException("El token no tiene sesion.");
        }
        String tenant = claims.get(CLAIM_TENANT, String.class);
        List<?> roles = claims.get(CLAIM_ROLES, List.class);
        return new UsuarioAutenticado(
                UUID.fromString(claims.getSubject()),
                tenant != null ? UUID.fromString(tenant) : null,
                UUID.fromString(sid),
                roles == null ? List.of() : roles.stream().map(String::valueOf).toList());
    }

    /** Valida un preauth token y devuelve el id de la cuenta. */
    public UUID validarPreauth(String token) {
        Claims claims = parse(token);
        if (!Boolean.TRUE.equals(claims.get(CLAIM_PREAUTH, Boolean.class))) {
            throw new IllegalArgumentException("El token no es de pre-autenticacion.");
        }
        return UUID.fromString(claims.getSubject());
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(props.issuer())
                .requireAudience(props.audience())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
