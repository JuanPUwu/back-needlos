package com.needlos.security.config;

import com.needlos.common.exception.CodigoError;
import com.needlos.common.web.CorrelationIdFilter;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.cors.DefaultCorsProcessor;
import tools.jackson.databind.ObjectMapper;

/**
 * Cuando el navegador envia un Origin no permitido, Spring rechaza la peticion aqui mismo (antes de
 * llegar a cualquier controlador o al GlobalExceptionHandler) y por defecto responde con texto
 * plano ("Invalid CORS request"). Esta clase lo sustituye por el mismo formato RFC 9457 + code que
 * usa el resto de la API (Reglas §6: un unico formato de error, siempre).
 */
class OrigenNoPermitidoCorsProcessor extends DefaultCorsProcessor {

    private final ObjectMapper objectMapper;

    OrigenNoPermitidoCorsProcessor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void rejectRequest(ServerHttpResponse response) throws IOException {
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("type", "about:blank");
        cuerpo.put("title", HttpStatus.FORBIDDEN.getReasonPhrase());
        cuerpo.put("status", HttpStatus.FORBIDDEN.value());
        cuerpo.put("detail", "Origen de la solicitud no permitido.");
        cuerpo.put("code", CodigoError.ORIGEN_NO_PERMITIDO.name());
        cuerpo.put("timestamp", Instant.now().toString());
        String correlationId = MDC.get(CorrelationIdFilter.MDC_CLAVE);
        if (correlationId != null) {
            cuerpo.put("correlationId", correlationId);
        }

        response.getBody().write(objectMapper.writeValueAsBytes(cuerpo));
        response.getBody().flush();
    }
}
