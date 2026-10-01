package com.needlos.soporte;

import com.needlos.security.acceso.Acceso;
import com.needlos.security.acceso.AccesoRepository;
import com.needlos.security.cuenta.Cuenta;
import com.needlos.security.cuenta.CuentaRepository;
import com.needlos.security.rol.RolRepository;
import com.needlos.tenant.TenantRepository;
import com.needlos.tenant.domain.PlanTenant;
import com.needlos.tenant.domain.Tenant;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;
import java.util.UUID;

/**
 * Crea datos aislados para cada prueba (correos y sastrerias unicos), de modo
 * que las pruebas no dependan unas de otras ni del orden de ejecucion.
 */
@TestComponent
public class DatosPrueba {

    public static final String CONTRASENA = "Clave123!";

    private final TenantRepository tenantRepo;
    private final CuentaRepository cuentaRepo;
    private final AccesoRepository accesoRepo;
    private final RolRepository rolRepo;
    private final PasswordEncoder passwordEncoder;

    public DatosPrueba(TenantRepository tenantRepo, CuentaRepository cuentaRepo, AccesoRepository accesoRepo,
                       RolRepository rolRepo, PasswordEncoder passwordEncoder) {
        this.tenantRepo = tenantRepo;
        this.cuentaRepo = cuentaRepo;
        this.accesoRepo = accesoRepo;
        this.rolRepo = rolRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public UUID crearSastreria() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setNombre("Sastreria " + sufijo);
        tenant.setSlug("sastreria-" + sufijo);
        tenant.setPlan(PlanTenant.DEMO);
        return tenantRepo.save(tenant).getId();
    }

    public void desactivarSastreria(UUID tenantId) {
        Tenant tenant = tenantRepo.findById(tenantId).orElseThrow();
        tenant.setActivo(false);
        tenantRepo.save(tenant);
    }

    public Cuenta crearCuenta() {
        return guardarCuenta(false);
    }

    /** Cuenta que solo entra con Google (sin contrasena), con acceso a una sastreria nueva. */
    public Cuenta crearCuentaSinContrasena() {
        Cuenta cuenta = guardarCuenta(false);
        cuenta.setPasswordHash(null);
        cuenta = cuentaRepo.save(cuenta);
        darAcceso(cuenta, crearSastreria(), "SASTRE_ADMIN");
        return cuenta;
    }

    public Cuenta crearSuperAdmin() {
        return guardarCuenta(true);
    }

    public Acceso darAcceso(Cuenta cuenta, UUID tenantId, String rol) {
        Acceso acceso = new Acceso();
        acceso.setId(UUID.randomUUID());
        acceso.setCuentaId(cuenta.getId());
        acceso.setTenantId(tenantId);
        acceso.setRoles(Set.of(rolRepo.findByNombre(rol).orElseThrow()));
        return accesoRepo.save(acceso);
    }

    public void desactivarAcceso(Acceso acceso) {
        Acceso actual = accesoRepo.findById(acceso.getId()).orElseThrow();
        actual.setActivo(false);
        accesoRepo.save(actual);
    }

    /** Cuenta SASTRE_ADMIN de una sastreria nueva. */
    public Cuenta crearDueno(UUID tenantId) {
        Cuenta cuenta = crearCuenta();
        darAcceso(cuenta, tenantId, "SASTRE_ADMIN");
        return cuenta;
    }

    private Cuenta guardarCuenta(boolean superAdmin) {
        Cuenta cuenta = new Cuenta();
        cuenta.setId(UUID.randomUUID());
        cuenta.setEmail("u-" + UUID.randomUUID() + "@prueba.com");
        cuenta.setPasswordHash(passwordEncoder.encode(CONTRASENA));
        cuenta.setNombre("Nombre");
        cuenta.setApellido("Apellido");
        cuenta.setSuperAdmin(superAdmin);
        return cuentaRepo.save(cuenta);
    }
}
