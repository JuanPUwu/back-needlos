package com.needlos.soporte;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import com.needlos.security.auth.CookieRefresh;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Base de las pruebas de integracion: aplicacion completa + PostgreSQL real (Testcontainers) +
 * MockMvc. El contexto se reutiliza entre clases.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfig.class, DatosPrueba.class})
public abstract class IntegracionTest {

    protected static final String ORIGEN = "http://localhost:4200";

    @Autowired protected MockMvc mvc;

    @Autowired protected DatosPrueba datos;

    protected ResultActions login(String email, String contrasena) throws Exception {
        return mvc.perform(
                post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                        {"email": "%s", "password": "%s"}
                        """
                                        .formatted(email, contrasena)));
    }

    /** Login de una cuenta con una sola sastreria: devuelve access token y cookie. */
    protected SesionPrueba iniciarSesion(String email) throws Exception {
        MvcResult resultado = login(email, DatosPrueba.CONTRASENA).andReturn();
        String body = resultado.getResponse().getContentAsString();
        return new SesionPrueba(
                JsonPath.read(body, "$.sesion.accessToken"),
                resultado.getResponse().getCookie(CookieRefresh.NOMBRE));
    }

    protected ResultActions refrescar(Cookie cookie) throws Exception {
        return mvc.perform(post("/api/v1/auth/refresh").header("Origin", ORIGEN).cookie(cookie));
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    /** Access token + cookie del refresh de una sesion abierta en la prueba. */
    public record SesionPrueba(String accessToken, Cookie cookie) {}
}
