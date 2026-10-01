package com.needlos.dev;

import com.needlos.common.tenant.TenantContext;
import com.needlos.security.acceso.Acceso;
import com.needlos.security.acceso.AccesoRepository;
import com.needlos.security.cuenta.Cuenta;
import com.needlos.security.cuenta.CuentaRepository;
import com.needlos.security.rol.Rol;
import com.needlos.security.rol.RolRepository;
import com.needlos.tenant.TenantRepository;
import com.needlos.tenant.domain.PlanTenant;
import com.needlos.tenant.domain.Tenant;
import com.needlos.tipoprenda.TipoPrendaRepository;
import com.needlos.tipoprenda.domain.TipoPrenda;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Datos de prueba para DESARROLLO (Reglas §8.4): solo existe en el perfil dev,
 * nunca en produccion. Se ejecuta una vez (si ya existen, no hace nada).
 *
 *  · Sastrerias: SastreriaPablo y SastreriaAngely, con un catalogo basico.
 *  · pablys8@gmail.com  -> SUPER_ADMIN (solo Google, sin contrasena).
 *  · admin@example.com  -> SASTRE_ADMIN de ambas sastrerias.
 *  · sastre@example.com -> SASTRE en ambas sastrerias.
 */
@Component
@Profile("dev")
public class DatosPruebaInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosPruebaInitializer.class);

    private static final UUID   TENANT_PABLO  = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID   TENANT_ANGELY = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final String PASSWORD      = "Test123!";

    private static final Map<String, BigDecimal> CATALOGO = Map.of(
            "Camisa",   new BigDecimal("18000"),
            "Pantalon", new BigDecimal("12000"),
            "Saco",     new BigDecimal("15000"),
            "Vestido",  new BigDecimal("12000"),
            "Chaqueta", new BigDecimal("15000"));

    private final TenantRepository     tenantRepo;
    private final TipoPrendaRepository tipoPrendaRepo;
    private final CuentaRepository     cuentaRepo;
    private final AccesoRepository     accesoRepo;
    private final RolRepository        rolRepo;
    private final PasswordEncoder      passwordEncoder;

    public DatosPruebaInitializer(TenantRepository tenantRepo,
                                  TipoPrendaRepository tipoPrendaRepo,
                                  CuentaRepository cuentaRepo,
                                  AccesoRepository accesoRepo,
                                  RolRepository rolRepo,
                                  PasswordEncoder passwordEncoder) {
        this.tenantRepo = tenantRepo;
        this.tipoPrendaRepo = tipoPrendaRepo;
        this.cuentaRepo = cuentaRepo;
        this.accesoRepo = accesoRepo;
        this.rolRepo = rolRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (tenantRepo.existsById(TENANT_PABLO)) {
            return;
        }

        crearSastreria(TENANT_PABLO, "SastreriaPablo", "sastreria-pablo");
        crearSastreria(TENANT_ANGELY, "SastreriaAngely", "sastreria-angely");

        Rol rolAdmin  = rolRepo.findByNombre("SASTRE_ADMIN").orElseThrow();
        Rol rolSastre = rolRepo.findByNombre("SASTRE").orElseThrow();

        Cuenta superAdmin = nuevaCuenta("pablys8@gmail.com", null, "Pablo", "SuperAdmin", null);
        superAdmin.setSuperAdmin(true);
        cuentaRepo.save(superAdmin);

        Cuenta admin = cuentaRepo.save(nuevaCuenta("admin@example.com", passwordEncoder.encode(PASSWORD),
                "Admin", "Ejemplo", "1000000001"));
        crearAcceso(admin.getId(), TENANT_PABLO, rolAdmin);
        crearAcceso(admin.getId(), TENANT_ANGELY, rolAdmin);

        Cuenta sastre = cuentaRepo.save(nuevaCuenta("sastre@example.com", passwordEncoder.encode(PASSWORD),
                "Sastre", "Ejemplo", "1000000002"));
        crearAcceso(sastre.getId(), TENANT_PABLO, rolSastre);
        crearAcceso(sastre.getId(), TENANT_ANGELY, rolSastre);

        log.info("Datos de prueba creados (perfil dev). Contrasena de las cuentas de prueba: {}", PASSWORD);
    }

    private void crearSastreria(UUID id, String nombre, String slug) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setNombre(nombre);
        tenant.setSlug(slug);
        tenant.setPlan(PlanTenant.DEMO);
        tenantRepo.save(tenant);

        // El catalogo pertenece a la sastreria: Hibernate toma el tenant del contexto.
        TenantContext.set(id, null);
        try {
            CATALOGO.forEach((nombrePrenda, precio) -> {
                TipoPrenda tipo = new TipoPrenda();
                tipo.setNombre(nombrePrenda);
                tipo.setPrecioBase(precio);
                tipoPrendaRepo.save(tipo);
            });
        } finally {
            TenantContext.clear();
        }
    }

    private Cuenta nuevaCuenta(String email, String passwordHash,
                               String nombre, String apellido, String documento) {
        Cuenta c = new Cuenta();
        c.setId(UUID.randomUUID());
        c.setEmail(email);
        c.setPasswordHash(passwordHash);
        c.setNombre(nombre);
        c.setApellido(apellido);
        c.setNumeroDocumento(documento);
        c.setActivo(true);
        return c;
    }

    private void crearAcceso(UUID cuentaId, UUID tenantId, Rol rol) {
        Acceso acceso = new Acceso();
        acceso.setId(UUID.randomUUID());
        acceso.setCuentaId(cuentaId);
        acceso.setTenantId(tenantId);
        acceso.setRoles(Set.of(rol));
        acceso.setActivo(true);
        accesoRepo.save(acceso);
    }
}
