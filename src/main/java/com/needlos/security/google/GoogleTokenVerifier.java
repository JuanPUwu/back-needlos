package com.needlos.security.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.needlos.common.exception.CodigoError;
import com.needlos.common.exception.NoAutenticadoException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Verifica el ID token que emite Google al iniciar sesion con Google. Comprueba firma, expiracion y
 * que la audiencia (aud) sea nuestro Client ID.
 */
@Service
public class GoogleTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerifier.class);

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifier(@Value("${needlos.google.client-id}") String clientId) {
        this.verifier =
                new GoogleIdTokenVerifier.Builder(
                                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                        .setAudience(Collections.singletonList(clientId))
                        .build();
    }

    public UsuarioGoogle verificar(String idTokenString) {
        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(idTokenString);
        } catch (IOException | GeneralSecurityException e) {
            // No se pudieron obtener/usar las claves publicas de Google: fallo de infraestructura.
            log.warn("No se pudo verificar un ID token de Google: {}", e.getMessage());
            throw invalido();
        } catch (IllegalArgumentException e) {
            // Token malformado.
            throw invalido();
        }
        if (idToken == null) {
            throw invalido();
        }

        Payload payload = idToken.getPayload();
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new NoAutenticadoException(
                    CodigoError.CREDENCIALES_INVALIDAS, "Tu correo de Google no esta verificado.");
        }

        return new UsuarioGoogle(
                payload.getSubject(),
                payload.getEmail(),
                (String) payload.get("given_name"),
                (String) payload.get("family_name"));
    }

    private NoAutenticadoException invalido() {
        return new NoAutenticadoException(
                CodigoError.CREDENCIALES_INVALIDAS,
                "No se pudo verificar tu cuenta de Google. Intentalo de nuevo.");
    }

    /** Datos de la persona extraidos del ID token de Google. */
    public record UsuarioGoogle(String sub, String email, String nombre, String apellido) {}
}
