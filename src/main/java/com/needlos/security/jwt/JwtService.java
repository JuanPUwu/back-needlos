package com.needlos.security.jwt;

import com.needlos.security.usuario.Rol;
import com.needlos.security.usuario.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Genera y valida el access token (JWT firmado con HS256).
 *
 * El token lleva en sus claims: sub (id de usuario), tenant_id y roles. Asi el
 * filtro reconstruye el contexto de seguridad sin ir a la base de datos en cada
 * request.
 */
@Service
public class JwtService {

    private static final String CLAIM_TENANT = "tenant_id";
    private static final String CLAIM_ROLES  = "roles";

    private final JwtProperties props;
    private final SecretKey key;

    public JwtService(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generarAccessToken(Usuario usuario) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plus(props.accessTokenMinutes(), ChronoUnit.MINUTES);

        List<String> roles = usuario.getRoles().stream()
                .map(Rol::getNombre)
                .toList();

        return Jwts.builder()
                .issuer(props.issuer())
                .audience().add(props.audience()).and()
                .subject(usuario.getId().toString())
                .claim(CLAIM_TENANT, usuario.getTenantId().toString())
                .claim(CLAIM_ROLES, roles)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(expira))
                .signWith(key)
                .compact();
    }

    public DatosToken validar(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(props.issuer())
                .requireAudience(props.audience())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        UUID usuarioId = UUID.fromString(claims.getSubject());
        UUID tenantId  = UUID.fromString(claims.get(CLAIM_TENANT, String.class));
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get(CLAIM_ROLES, List.class);

        return new DatosToken(usuarioId, tenantId, roles == null ? List.of() : roles);
    }

    /** Datos extraidos de un access token valido. */
    public record DatosToken(UUID usuarioId, UUID tenantId, List<String> roles) {
    }
}
