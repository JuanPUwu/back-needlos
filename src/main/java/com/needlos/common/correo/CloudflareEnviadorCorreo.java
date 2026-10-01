package com.needlos.common.correo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Envia correos con la API de envio de Cloudflare Email (cuenta y dominio
 * needlos.com ya configurados en Cloudflare). Si falta el token (solo posible
 * en dev, sin .env) no envia y lo advierte en el log; nunca lanza: el envio de
 * un correo nunca debe bloquear al usuario (Reglas §10).
 */
@Component
class CloudflareEnviadorCorreo implements EnviadorCorreo {

    private static final Logger log = LoggerFactory.getLogger(CloudflareEnviadorCorreo.class);
    private static final String RUTA = "/email/sending/send";

    private final RestClient restClient;
    private final boolean configurado;

    CloudflareEnviadorCorreo(RestClient.Builder restClientBuilder,
                             @Value("${needlos.correo.cloudflare.account-id:}") String accountId,
                             @Value("${needlos.correo.cloudflare.token:}") String token) {
        this.configurado = !accountId.isBlank() && !token.isBlank();
        this.restClient = restClientBuilder
                .baseUrl("https://api.cloudflare.com/client/v4/accounts/" + accountId + RUTA)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
    }

    @Override
    public void enviar(Correo correo) {
        if (!configurado) {
            log.warn("Envio de correos desactivado (falta CLOUDFLARE_ACCOUNT_ID/CLOUDFLARE_EMAIL_TOKEN en .env): "
                    + "no se envio \"{}\"", correo.asunto());
            return;
        }

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("to", correo.para());
        cuerpo.put("from", correo.remitente().direccion());
        if (correo.remitente().replyTo() != null) {
            cuerpo.put("reply_to", correo.remitente().replyTo());
        }
        cuerpo.put("subject", correo.asunto());
        cuerpo.put("html", correo.html());
        cuerpo.put("text", correo.texto());

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> respuesta = restClient.post().body(cuerpo).retrieve().body(Map.class);
            if (respuesta != null && Boolean.TRUE.equals(respuesta.get("success"))) {
                log.info("Correo enviado: \"{}\"", correo.asunto());
            } else {
                log.error("Cloudflare rechazo el correo \"{}\": {}", correo.asunto(),
                        respuesta == null ? null : respuesta.get("errors"));
            }
        } catch (RestClientException e) {
            log.error("No se pudo enviar el correo \"{}\" (Cloudflare Email)", correo.asunto(), e);
        }
    }
}
