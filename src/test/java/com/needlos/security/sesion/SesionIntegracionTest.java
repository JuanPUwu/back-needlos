package com.needlos.security.sesion;

import com.jayway.jsonpath.JsonPath;
import com.needlos.security.acceso.Acceso;
import com.needlos.security.auth.CookieRefresh;
import com.needlos.security.cuenta.Cuenta;
import com.needlos.soporte.DatosPrueba;
import com.needlos.soporte.IntegracionTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SesionIntegracionTest extends IntegracionTest {

    @Autowired
    private SesionRepository sesionRepo;

    @Test
    void refresh_rotaElTokenYEntregaNuevoAccessToken() throws Exception {
        Cuenta dueno = datos.crearDueno(datos.crearSastreria());
        SesionPrueba sesion = iniciarSesion(dueno.getEmail());

        MvcResult resultado = refrescar(sesion.cookie())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andReturn();

        Cookie nueva = resultado.getResponse().getCookie(CookieRefresh.NOMBRE);
        assertThat(nueva).isNotNull();
        assertThat(nueva.getValue()).isNotEqualTo(sesion.cookie().getValue());
        refrescar(nueva).andExpect(status().isOk());
    }

    @Test
    void refreshSinOrigenPermitido_responde403() throws Exception {
        Cuenta dueno = datos.crearDueno(datos.crearSastreria());
        SesionPrueba sesion = iniciarSesion(dueno.getEmail());

        mvc.perform(post("/api/v1/auth/refresh").cookie(sesion.cookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ORIGEN_NO_PERMITIDO"));
        mvc.perform(post("/api/v1/auth/refresh").header("Origin", "https://atacante.com").cookie(sesion.cookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ORIGEN_NO_PERMITIDO"));
    }

    @Test
    void refreshSinCookie_responde401() throws Exception {
        mvc.perform(post("/api/v1/auth/refresh").header("Origin", ORIGEN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("SESION_INVALIDA"));
    }

    @Test
    void refreshAnteriorDentroDeLaGracia_seAceptaSinRotarDeNuevo() throws Exception {
        Cuenta dueno = datos.crearDueno(datos.crearSastreria());
        SesionPrueba sesion = iniciarSesion(dueno.getEmail());
        refrescar(sesion.cookie()).andExpect(status().isOk());

        // Otra pestana llega con el token que se acaba de rotar.
        refrescar(sesion.cookie())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void reusoDeRefreshRotado_revocaTodasLasSesionesDeLaCuenta() throws Exception {
        Cuenta dueno = datos.crearDueno(datos.crearSastreria());
        SesionPrueba robada = iniciarSesion(dueno.getEmail());
        SesionPrueba otroDispositivo = iniciarSesion(dueno.getEmail());
        refrescar(robada.cookie()).andExpect(status().isOk());

        // Simula que paso la ventana de gracia desde la rotacion.
        Sesion rotada =
                sesionRepo.findByTokenHashAnterior(hashDe(robada.cookie())).orElseThrow();
        rotada.setRotadaEn(Instant.now().minusSeconds(3600));
        sesionRepo.save(rotada);

        refrescar(robada.cookie())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("SESION_INVALIDA"));

        mvc.perform(get("/api/v1/auth/sesiones").header("Authorization", bearer(otroDispositivo.accessToken())))
                .andExpect(status().isUnauthorized());
        refrescar(otroDispositivo.cookie()).andExpect(status().isUnauthorized());
    }

    @Test
    void logout_invalidaAlInstanteElAccessTokenYBorraLaCookie() throws Exception {
        Cuenta dueno = datos.crearDueno(datos.crearSastreria());
        SesionPrueba sesion = iniciarSesion(dueno.getEmail());
        mvc.perform(get("/api/v1/clientes").header("Authorization", bearer(sesion.accessToken())))
                .andExpect(status().isOk());

        MvcResult resultado = mvc.perform(post("/api/v1/auth/logout").header("Origin", ORIGEN).cookie(sesion.cookie()))
                .andExpect(status().isNoContent())
                .andReturn();
        assertThat(resultado.getResponse().getCookie(CookieRefresh.NOMBRE).getMaxAge()).isZero();

        mvc.perform(get("/api/v1/clientes").header("Authorization", bearer(sesion.accessToken())))
                .andExpect(status().isUnauthorized());
        refrescar(sesion.cookie()).andExpect(status().isUnauthorized());
    }

    @Test
    void misSesiones_listaYCierraOtrosDispositivos() throws Exception {
        Cuenta dueno = datos.crearDueno(datos.crearSastreria());
        SesionPrueba actual = iniciarSesion(dueno.getEmail());
        SesionPrueba otra = iniciarSesion(dueno.getEmail());

        String body = mvc.perform(get("/api/v1/auth/sesiones").header("Authorization", bearer(actual.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.actual == true)]", hasSize(1)))
                .andReturn().getResponse().getContentAsString();
        List<String> otras = JsonPath.read(body, "$[?(@.actual == false)].id");

        mvc.perform(delete("/api/v1/auth/sesiones/" + otras.get(0)).header("Authorization", bearer(actual.accessToken())))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/auth/sesiones").header("Authorization", bearer(otra.accessToken())))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/sesiones").header("Authorization", bearer(actual.accessToken())))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void cerrarSesionDeOtraCuenta_responde404() throws Exception {
        Cuenta yo = datos.crearDueno(datos.crearSastreria());
        Cuenta otro = datos.crearDueno(datos.crearSastreria());
        SesionPrueba miSesion = iniciarSesion(yo.getEmail());
        SesionPrueba suSesion = iniciarSesion(otro.getEmail());
        String body = mvc.perform(get("/api/v1/auth/sesiones").header("Authorization", bearer(suSesion.accessToken())))
                .andReturn().getResponse().getContentAsString();
        String suId = JsonPath.read(body, "$[0].id");

        mvc.perform(delete("/api/v1/auth/sesiones/" + suId).header("Authorization", bearer(miSesion.accessToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESION_NO_ENCONTRADA"));
    }

    @Test
    void accesoRevocado_impideRenovarLaSesion() throws Exception {
        UUID tenant = datos.crearSastreria();
        Cuenta sastre = datos.crearCuenta();
        Acceso acceso = datos.darAcceso(sastre, tenant, "SASTRE");
        SesionPrueba sesion = iniciarSesion(sastre.getEmail());

        datos.desactivarAcceso(acceso);

        refrescar(sesion.cookie())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SIN_ACCESO_SASTRERIA"));
        refrescar(sesion.cookie()).andExpect(status().isUnauthorized());
    }

    @Test
    void cambiarContrasena_validaLaActualYCierraLasDemasSesiones() throws Exception {
        Cuenta dueno = datos.crearDueno(datos.crearSastreria());
        SesionPrueba actual = iniciarSesion(dueno.getEmail());
        SesionPrueba otra = iniciarSesion(dueno.getEmail());

        mvc.perform(patch("/api/v1/cuenta/contrasena").header("Authorization", bearer(actual.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contrasenaActual": "Equivocada1!", "contrasenaNueva": "NuevaClave1!"}"""))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("CONTRASENA_ACTUAL_INCORRECTA"));

        mvc.perform(patch("/api/v1/cuenta/contrasena").header("Authorization", bearer(actual.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contrasenaActual": "%s", "contrasenaNueva": "debil"}""".formatted(DatosPrueba.CONTRASENA)))
                .andExpect(status().isBadRequest());

        mvc.perform(patch("/api/v1/cuenta/contrasena").header("Authorization", bearer(actual.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contrasenaActual": "%s", "contrasenaNueva": "NuevaClave1!"}""".formatted(DatosPrueba.CONTRASENA)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/auth/sesiones").header("Authorization", bearer(otra.accessToken())))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/sesiones").header("Authorization", bearer(actual.accessToken())))
                .andExpect(status().isOk());
        login(dueno.getEmail(), "NuevaClave1!").andExpect(status().isOk());
    }

    private static String hashDe(Cookie cookie) throws Exception {
        byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest(cookie.getValue().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return java.util.HexFormat.of().formatHex(digest);
    }
}
