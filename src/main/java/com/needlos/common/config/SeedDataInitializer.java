package com.needlos.common.config;

import com.needlos.security.usuario.Rol;
import com.needlos.security.usuario.RolRepository;
import com.needlos.security.usuario.Usuario;
import com.needlos.security.usuario.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * Crea, la primera vez que arranca, el usuario dueno (SASTRE_ADMIN) del tenant
 * de demostracion. La contrasena se hashea con BCrypt (por eso no se siembra en
 * el script SQL de Flyway).
 */
@Component
public class SeedDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDataInitializer.class);

    private static final UUID   TENANT_DEMO    = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String ADMIN_EMAIL    = "admin@demo.com";
    private static final String ADMIN_PASSWORD = "Admin123!";

    private final UsuarioRepository usuarioRepo;
    private final RolRepository     rolRepo;
    private final PasswordEncoder   passwordEncoder;

    public SeedDataInitializer(UsuarioRepository usuarioRepo,
                               RolRepository rolRepo,
                               PasswordEncoder passwordEncoder) {
        this.usuarioRepo = usuarioRepo;
        this.rolRepo = rolRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepo.existsByTenantIdAndEmailIgnoreCase(TENANT_DEMO, ADMIN_EMAIL)) {
            return;
        }

        Rol rolDueno = rolRepo.findByNombre("SASTRE_ADMIN").orElseThrow();

        Usuario admin = new Usuario();
        admin.setId(UUID.randomUUID());
        admin.setTenantId(TENANT_DEMO);
        admin.setNombre("Admin");
        admin.setApellido("Demo");
        admin.setNumeroDocumento("0000000000");
        admin.setEmail(ADMIN_EMAIL);
        admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setActivo(true);
        admin.setRoles(Set.of(rolDueno));
        usuarioRepo.save(admin);

        log.info("=================================================================");
        log.info(" Usuario dueno de demostracion creado (rol SASTRE_ADMIN)");
        log.info("   slug: demo | email: {} | password: {}", ADMIN_EMAIL, ADMIN_PASSWORD);
        log.info("=================================================================");
    }
}
