package com.needlos.security.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Tokens opacos de un solo proposito (refresh, recuperacion de contrasena): 256 bits aleatorios; en
 * la BD solo se guarda su hash SHA-256.
 */
public final class TokenAleatorio {

    private static final int BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private TokenAleatorio() {}

    /** Token nuevo en Base64 URL-safe (apto para cookies y enlaces). */
    public static String nuevo() {
        byte[] bytes = new byte[BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String token) {
        try {
            byte[] digest =
                    MessageDigest.getInstance("SHA-256")
                            .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
