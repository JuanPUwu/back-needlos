package com.needlos.common.exception;

import com.needlos.common.web.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Traduce toda excepcion a un unico formato: RFC 9457 (ProblemDetail) + "code".
 *
 * <pre>
 * { "type", "title", "status", "detail", "instance", "code", "timestamp", "correlationId" }
 * </pre>
 *
 * Los errores esperados (4xx) se registran en INFO; los inesperados (5xx) en
 * ERROR con la excepcion completa. Al cliente nunca le llegan stack traces,
 * SQL ni nombres de clases.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── Excepciones propias ─────────────────────────────────────────

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> api(ApiException ex, WebRequest request) {
        log.info("{} {} en {}: {}", ex.getStatus().value(), ex.getCodigo(), ruta(request), ex.getMessage());
        ResponseEntity.BodyBuilder respuesta = ResponseEntity.status(ex.getStatus());
        if (ex instanceof DemasiadasSolicitudesException limite) {
            respuesta.header(HttpHeaders.RETRY_AFTER, String.valueOf(limite.getReintentarEnSegundos()));
        }
        return respuesta.body(problema(ex.getStatus(), ex.getCodigo(), ex.getMessage(), request));
    }

    // ── Seguridad ───────────────────────────────────────────────────

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> noAutenticado(AuthenticationException ex, WebRequest request) {
        return responder(HttpStatus.UNAUTHORIZED, CodigoError.NO_AUTENTICADO,
                "Debes iniciar sesion para continuar.", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> sinPermiso(AccessDeniedException ex, WebRequest request) {
        return responder(HttpStatus.FORBIDDEN, CodigoError.SIN_PERMISO,
                "No tienes permisos para realizar esta accion.", request);
    }

    // ── Persistencia ────────────────────────────────────────────────

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> concurrencia(ObjectOptimisticLockingFailureException ex,
                                                      WebRequest request) {
        return responder(HttpStatus.CONFLICT, CodigoError.CONFLICTO_CONCURRENCIA,
                "Otra persona modifico este registro al mismo tiempo. Recarga e intentalo de nuevo.", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> integridad(DataIntegrityViolationException ex, WebRequest request) {
        // El detalle (constraint, SQL) solo va al log, nunca al cliente.
        log.warn("Violacion de integridad en {}: {}", ruta(request), ex.getMostSpecificCause().getMessage());
        return responder(HttpStatus.CONFLICT, CodigoError.CONFLICTO_DATOS,
                "La operacion entra en conflicto con datos existentes.", request);
    }

    // ── Inesperados ─────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> inesperado(Exception ex, WebRequest request) {
        log.error("Error inesperado en {}", ruta(request), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, CodigoError.ERROR_INTERNO,
                "Ha ocurrido un error inesperado. Si persiste, comunicate con soporte.", request);
    }

    // ── Excepciones de Spring MVC (400, 404, 405, 415...) ───────────

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<Map<String, String>> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(this::errorDeCampo)
                .toList();
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, CodigoError.VALIDACION,
                "Hay datos invalidos en la solicitud.", request);
        problema.setProperty("errores", errores);
        log.info("400 VALIDACION en {}: {}", ruta(request), errores);
        return ResponseEntity.badRequest().body(problema);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex,
                                                             @Nullable Object body,
                                                             HttpHeaders headers,
                                                             HttpStatusCode statusCode,
                                                             WebRequest request) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        CodigoError codigo = switch (status) {
            case NOT_FOUND -> CodigoError.RECURSO_NO_ENCONTRADO;
            case METHOD_NOT_ALLOWED -> CodigoError.METODO_NO_PERMITIDO;
            case UNSUPPORTED_MEDIA_TYPE, NOT_ACCEPTABLE -> CodigoError.FORMATO_NO_SOPORTADO;
            case INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE -> CodigoError.ERROR_INTERNO;
            default -> CodigoError.SOLICITUD_INVALIDA;
        };
        String mensaje = switch (codigo) {
            case RECURSO_NO_ENCONTRADO -> "El recurso solicitado no existe.";
            case METODO_NO_PERMITIDO -> "Metodo HTTP no permitido para este recurso.";
            case FORMATO_NO_SOPORTADO -> "Formato de contenido no soportado.";
            case ERROR_INTERNO -> "Ha ocurrido un error inesperado. Si persiste, comunicate con soporte.";
            default -> "La solicitud no es valida. Revisa los datos enviados.";
        };
        if (status.is5xxServerError()) {
            log.error("Error de infraestructura en {}", ruta(request), ex);
        } else {
            log.info("{} {} en {}: {}", status.value(), codigo, ruta(request), ex.getMessage());
        }
        return ResponseEntity.status(status).headers(headers).body(problema(status, codigo, mensaje, request));
    }

    // ── Construccion del ProblemDetail ──────────────────────────────

    private ResponseEntity<ProblemDetail> responder(HttpStatus status, CodigoError codigo,
                                                    String mensaje, WebRequest request) {
        log.info("{} {} en {}", status.value(), codigo, ruta(request));
        return ResponseEntity.status(status).body(problema(status, codigo, mensaje, request));
    }

    private ProblemDetail problema(HttpStatus status, CodigoError codigo, String mensaje, WebRequest request) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, mensaje);
        problema.setTitle(status.getReasonPhrase());
        problema.setInstance(URI.create(ruta(request)));
        problema.setProperty("code", codigo.name());
        problema.setProperty("timestamp", Instant.now().toString());
        String correlationId = MDC.get(CorrelationIdFilter.MDC_CLAVE);
        if (correlationId != null) {
            problema.setProperty("correlationId", correlationId);
        }
        return problema;
    }

    private Map<String, String> errorDeCampo(FieldError fe) {
        String mensaje = fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Valor invalido.";
        return Map.of("campo", fe.getField(), "mensaje", mensaje);
    }

    private String ruta(WebRequest request) {
        if (request instanceof ServletWebRequest servlet) {
            return servlet.getRequest().getRequestURI();
        }
        return "";
    }
}
