package com.needlos.security.auth;

import com.jayway.jsonpath.JsonPath;
import com.needlos.security.acceso.Acceso;
import com.needlos.security.acceso.AccesoRepository;
import com.needlos.security.cuenta.Cuenta;
import com.needlos.security.cuenta.CuentaRepository;
import com.needlos.soporte.DatosPrueba;
import com.needlos.soporte.IntegracionTest;
import com.needlos.tenant.TenantRepository;
import com.needlos.tenant.domain.Tenant;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegracionTest extends IntegracionTest {

    @Autowired
    private CuentaRepository cuentaRepo;
    @Autowired
    private AccesoRepository accesoRepo;
    @Autowired
    private TenantRepository tenantRepo;

    @Test
    void loginConUnaSastreria_entregaAccessTokenEnCuerpoYRefreshSoloEnCookieHttpOnly() throws Exception {
        UUID tenant = datos.crearSastreria();
        Cuenta dueno = datos.crearDueno(tenant);

        MvcResult resultado = login(dueno.getEmail(), DatosPrueba.CONTRASENA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiereSeleccion").value(false))
                .andExpect(jsonPath("$.sesion.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.sesion.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.sesion.roles[0]").value("SASTRE_ADMIN"))
                .andExpect(jsonPath("$.sesion.sastreria.tenantId").value(tenant.toString()))
                .andExpect(jsonPath("$.sesion.cuenta.email").value(dueno.getEmail()))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")))
                .andReturn();

        Cookie cookie = resultado.getResponse().getCookie(CookieRefresh.NOMBRE);
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");
        assertThat(cookie.getMaxAge()).isPositive();
    }

    @Test
    void contrasenaIncorrecta_responde401ConFormatoEstandar() throws Exception {
        Cuenta dueno = datos.crearDueno(datos.crearSastreria());

        login(dueno.getEmail(), "Incorrecta1!")
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("CREDENCIALES_INVALIDAS"))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.instance").value("/api/v1/auth/login"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void correoInexistente_respondeIgualQueContrasenaIncorrecta() throws Exception {
        login("no-existe-" + UUID.randomUUID() + "@prueba.com", DatosPrueba.CONTRASENA)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    void cuentaSinAccesos_responde403() throws Exception {
        Cuenta sinAcceso = datos.crearCuenta();

        login(sinAcceso.getEmail(), DatosPrueba.CONTRASENA)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SIN_ACCESO_SASTRERIA"));
    }

    @Test
    void accesoASastreriaDesactivada_noCuenta() throws Exception {
        UUID tenant = datos.crearSastreria();
        Cuenta dueno = datos.crearDueno(tenant);
        datos.desactivarSastreria(tenant);

        login(dueno.getEmail(), DatosPrueba.CONTRASENA)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SIN_ACCESO_SASTRERIA"));
    }

    @Test
    void cuentaConVariasSastrerias_eligeUnaConPreauth() throws Exception {
        UUID tenantA = datos.crearSastreria();
        UUID tenantB = datos.crearSastreria();
        UUID ajena = datos.crearSastreria();
        Cuenta sastre = datos.crearCuenta();
        datos.darAcceso(sastre, tenantA, "SASTRE");
        datos.darAcceso(sastre, tenantB, "SASTRE");

        String body = login(sastre.getEmail(), DatosPrueba.CONTRASENA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiereSeleccion").value(true))
                .andExpect(jsonPath("$.sastrerias", hasSize(2)))
                .andExpect(jsonPath("$.sesion").doesNotExist())
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andReturn().getResponse().getContentAsString();
        String preauth = JsonPath.read(body, "$.preauthToken");

        mvc.perform(post("/api/v1/auth/seleccionar-sastreria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"preauthToken": "%s", "tenantId": "%s"}""".formatted(preauth, ajena)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SIN_ACCESO_SASTRERIA"));

        mvc.perform(post("/api/v1/auth/seleccionar-sastreria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"preauthToken": "%s", "tenantId": "%s"}""".formatted(preauth, tenantB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sastreria.tenantId").value(tenantB.toString()))
                .andExpect(header().string("Set-Cookie", containsString(CookieRefresh.NOMBRE)));
    }

    @Test
    void preauthToken_noSirveComoAccessToken() throws Exception {
        Cuenta sastre = datos.crearCuenta();
        datos.darAcceso(sastre, datos.crearSastreria(), "SASTRE");
        datos.darAcceso(sastre, datos.crearSastreria(), "SASTRE");
        String body = login(sastre.getEmail(), DatosPrueba.CONTRASENA).andReturn().getResponse().getContentAsString();
        String preauth = JsonPath.read(body, "$.preauthToken");

        mvc.perform(get("/api/v1/clientes").header("Authorization", bearer(preauth)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
    }

    @Test
    void superAdmin_entraSinSastreria() throws Exception {
        Cuenta superAdmin = datos.crearSuperAdmin();

        login(superAdmin.getEmail(), DatosPrueba.CONTRASENA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sesion.sastreria").doesNotExist())
                .andExpect(jsonPath("$.sesion.roles[0]").value("SUPER_ADMIN"));
    }

    @Test
    void datosInvalidos_responde400ConErroresPorCampo() throws Exception {
        login("no-es-un-correo", "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"))
                .andExpect(jsonPath("$.errores[?(@.campo == 'email')]").exists())
                .andExpect(jsonPath("$.errores[?(@.campo == 'password')]").exists());
    }

    @Test
    void cuerpoIlegible_responde400SinDetallesTecnicos() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"));
    }

    @Test
    void endpointProtegidoSinToken_responde401() throws Exception {
        mvc.perform(get("/api/v1/clientes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
    }

    @Test
    void tokenManipulado_responde401() throws Exception {
        Cuenta dueno = datos.crearDueno(datos.crearSastreria());
        String token = iniciarSesion(dueno.getEmail()).accessToken();
        String manipulado = token.substring(0, token.length() - 4) + "abcd";

        mvc.perform(get("/api/v1/clientes").header("Authorization", bearer(manipulado)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registroDeSastreria_soloPideElNombreYGeneraElIdentificadorYQuedaSinVerificar() throws Exception {
        String email = "dueno-" + UUID.randomUUID() + "@prueba.com";
        String cuerpo = registro("Sastrería Ñandú & Hijos", email);

        mvc.perform(post("/api/v1/auth/registrar-sastreria").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(header().doesNotExist("Set-Cookie"));

        assertThat(tenantDe(email).getNombre()).isEqualTo("Sastrería Ñandú & Hijos");
        assertThat(tenantDe(email).getSlug()).startsWith("sastreria-nandu-hijos");
        assertThat(cuentaRepo.findByEmailIgnoreCase(email).orElseThrow().isVerificada()).isFalse();

        // Sin verificar, ni siquiera la contrasena correcta deja entrar.
        login(email, "Clave123!")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("CUENTA_SIN_VERIFICAR"));

        mvc.perform(post("/api/v1/auth/registrar-sastreria").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CORREO_EN_USO"));
    }

    @Test
    void dosSastreriasConElMismoNombre_recibenIdentificadoresDistintos() throws Exception {
        String nombre = "Taller " + UUID.randomUUID().toString().substring(0, 6);
        String emailA = "a-" + UUID.randomUUID() + "@prueba.com";
        String emailB = "b-" + UUID.randomUUID() + "@prueba.com";

        mvc.perform(post("/api/v1/auth/registrar-sastreria").contentType(MediaType.APPLICATION_JSON)
                        .content(registro(nombre, emailA)))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/auth/registrar-sastreria").contentType(MediaType.APPLICATION_JSON)
                        .content(registro(nombre, emailB)))
                .andExpect(status().isCreated());

        String slugA = tenantDe(emailA).getSlug();
        String slugB = tenantDe(emailB).getSlug();
        assertThat(slugA).isNotEqualTo(slugB);
        assertThat(slugB).startsWith(slugA + "-");
    }

    private Tenant tenantDe(String email) {
        Cuenta cuenta = cuentaRepo.findByEmailIgnoreCase(email).orElseThrow();
        Acceso acceso = accesoRepo.findVigentes(cuenta.getId()).stream().findFirst().orElseThrow();
        return tenantRepo.findById(acceso.getTenantId()).orElseThrow();
    }

    @Test
    void intentosFallidosSeguidos_bloqueanTemporalmenteElLoginConContrasena() throws Exception {
        Cuenta dueno = datos.crearDueno(datos.crearSastreria());
        for (int i = 1; i < 5; i++) {
            login(dueno.getEmail(), "Incorrecta1!").andExpect(status().isUnauthorized());
        }

        login(dueno.getEmail(), "Incorrecta1!")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("CUENTA_BLOQUEADA_TEMPORALMENTE"))
                .andExpect(header().exists("Retry-After"));

        // Ni la contrasena correcta entra mientras dure el bloqueo.
        login(dueno.getEmail(), DatosPrueba.CONTRASENA)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("CUENTA_BLOQUEADA_TEMPORALMENTE"));
    }

    @Test
    void correoInexistente_seBloqueaIgualQueUnoRegistrado() throws Exception {
        String inexistente = "nadie-" + UUID.randomUUID() + "@prueba.com";
        for (int i = 1; i < 5; i++) {
            login(inexistente, "Incorrecta1!").andExpect(status().isUnauthorized());
        }
        login(inexistente, "Incorrecta1!")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("CUENTA_BLOQUEADA_TEMPORALMENTE"));
    }

    @Test
    void loginExitoso_reiniciaElContadorDeFallos() throws Exception {
        Cuenta dueno = datos.crearDueno(datos.crearSastreria());
        for (int i = 1; i < 5; i++) {
            login(dueno.getEmail(), "Incorrecta1!").andExpect(status().isUnauthorized());
        }
        login(dueno.getEmail(), DatosPrueba.CONTRASENA).andExpect(status().isOk());

        for (int i = 1; i < 5; i++) {
            login(dueno.getEmail(), "Incorrecta1!").andExpect(status().isUnauthorized());
        }
        login(dueno.getEmail(), DatosPrueba.CONTRASENA).andExpect(status().isOk());
    }

    @Test
    void registroConContrasenaDebil_responde400() throws Exception {
        mvc.perform(post("/api/v1/auth/registrar-sastreria").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombreSastreria": "X", "nombreAdmin": "Ana", "apellidoAdmin": "Perez",
                         "numeroDocumento": "1", "email": "debil@prueba.com", "password": "12345678"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[?(@.campo == 'password')]").exists());
    }

    private static String registro(String nombreSastreria, String email) {
        return """
                {"nombreSastreria": "%s", "nombreAdmin": "Ana", "apellidoAdmin": "Perez",
                 "numeroDocumento": "123", "email": "%s", "password": "Clave123!"}
                """.formatted(nombreSastreria, email);
    }
}
