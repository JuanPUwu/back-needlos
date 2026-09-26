package com.needlos.security.auth;

import com.needlos.common.exception.BusinessException;
import com.needlos.common.exception.NotFoundException;
import com.needlos.security.auth.dto.AuthDtos.LoginRequest;
import com.needlos.security.auth.dto.AuthDtos.RegistrarSastreriaRequest;
import com.needlos.security.auth.dto.AuthDtos.TokenResponse;
import com.needlos.security.jwt.JwtProperties;
import com.needlos.security.jwt.JwtService;
import com.needlos.security.usuario.Rol;
import com.needlos.security.usuario.RolRepository;
import com.needlos.security.usuario.Usuario;
import com.needlos.security.usuario.UsuarioRepository;
import com.needlos.tenant.TenantRepository;
import com.needlos.tenant.domain.PlanTenant;
import com.needlos.tenant.domain.Tenant;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {

    /** Rol del dueno de la sastreria (quien la registra). */
    private static final String ROL_DUENO = "SASTRE_ADMIN";

    private final TenantRepository      tenantRepo;
    private final UsuarioRepository     usuarioRepo;
    private final RolRepository         rolRepo;
    private final RefreshTokenRepository refreshRepo;
    private final PasswordEncoder       passwordEncoder;
    private final JwtService            jwtService;
    private final JwtProperties         jwtProps;

    public AuthService(TenantRepository tenantRepo,
                       UsuarioRepository usuarioRepo,
                       RolRepository rolRepo,
                       RefreshTokenRepository refreshRepo,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       JwtProperties jwtProps) {
        this.tenantRepo = tenantRepo;
        this.usuarioRepo = usuarioRepo;
        this.rolRepo = rolRepo;
        this.refreshRepo = refreshRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProps = jwtProps;
    }

    /** Onboarding SaaS: crea una sastreria (plan DEMO) con su usuario dueno (SASTRE_ADMIN). */
    @Transactional
    public TokenResponse registrarSastreria(RegistrarSastreriaRequest req) {
        if (tenantRepo.existsBySlug(req.slug())) {
            throw new BusinessException("Ya existe una sastreria con el slug '" + req.slug() + "'.");
        }

        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setNombre(req.nombreSastreria());
        tenant.setSlug(req.slug());
        tenant.setPlan(PlanTenant.DEMO);
        tenant.setActivo(true);
        tenantRepo.save(tenant);

        Rol rolDueno = rolRepo.findByNombre(ROL_DUENO)
                .orElseThrow(() -> new NotFoundException("Rol " + ROL_DUENO + " no configurado."));

        Usuario dueno = new Usuario();
        dueno.setId(UUID.randomUUID());
        dueno.setTenantId(tenant.getId());
        dueno.setNombre(req.nombreAdmin());
        dueno.setApellido(req.apellidoAdmin());
        dueno.setNumeroDocumento(req.numeroDocumento());
        dueno.setEmail(req.email().toLowerCase());
        dueno.setPasswordHash(passwordEncoder.encode(req.password()));
        dueno.setTelefono(req.telefono());
        dueno.setActivo(true);
        dueno.setRoles(Set.of(rolDueno));
        usuarioRepo.save(dueno);

        return emitirTokens(dueno);
    }

    @Transactional
    public TokenResponse login(LoginRequest req) {
        Tenant tenant = tenantRepo.findBySlugAndActivoTrue(req.slug())
                .orElseThrow(() -> new BadCredentialsException("Credenciales invalidas."));

        Usuario usuario = usuarioRepo
                .findByTenantIdAndEmailIgnoreCase(tenant.getId(), req.email())
                .orElseThrow(() -> new BadCredentialsException("Credenciales invalidas."));

        if (!usuario.isActivo() || !passwordEncoder.matches(req.password(), usuario.getPasswordHash())) {
            throw new BadCredentialsException("Credenciales invalidas.");
        }

        return emitirTokens(usuario);
    }

    /** Rota el refresh token: revoca el anterior y emite uno nuevo. */
    @Transactional
    public TokenResponse refresh(String refreshTokenPlano) {
        String hash = TokenHasher.sha256(refreshTokenPlano);
        RefreshToken token = refreshRepo.findByTokenHash(hash)
                .orElseThrow(() -> new BadCredentialsException("Refresh token invalido."));

        if (!token.esValido()) {
            throw new BadCredentialsException("Refresh token expirado o revocado.");
        }

        token.setRevocado(true);
        refreshRepo.save(token);

        Usuario usuario = usuarioRepo.findById(token.getUsuarioId())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado."));

        return emitirTokens(usuario);
    }

    @Transactional
    public void logout(String refreshTokenPlano) {
        String hash = TokenHasher.sha256(refreshTokenPlano);
        refreshRepo.findByTokenHash(hash).ifPresent(t -> {
            t.setRevocado(true);
            refreshRepo.save(t);
        });
    }

    private TokenResponse emitirTokens(Usuario usuario) {
        String accessToken = jwtService.generarAccessToken(usuario);

        String refreshPlano = UUID.randomUUID().toString() + UUID.randomUUID();
        RefreshToken rt = new RefreshToken();
        rt.setId(UUID.randomUUID());
        rt.setUsuarioId(usuario.getId());
        rt.setTokenHash(TokenHasher.sha256(refreshPlano));
        rt.setExpira(Instant.now().plus(jwtProps.refreshTokenDays(), ChronoUnit.DAYS));
        rt.setRevocado(false);
        refreshRepo.save(rt);

        return new TokenResponse(accessToken, refreshPlano, jwtProps.accessTokenMinutes() * 60);
    }
}
