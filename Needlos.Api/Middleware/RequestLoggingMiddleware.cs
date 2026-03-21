using System.Diagnostics;

namespace Needlos.Api.Middleware;

/// <summary>
/// Middleware que registra cada request HTTP con su resultado y duración.
///
/// Loguea al finalizar el request (no al inicio) para poder incluir el status code.
/// El CorrelationId lo lee de HttpContext.Items, donde lo depositó CorrelationIdMiddleware.
/// El TenantId lo lee del claim "tenant_id" del JWT, si el usuario está autenticado.
///
/// Formato de log (estructurado):
///   HTTP {Method} {Path} → {StatusCode} en {ElapsedMs}ms | correlationId={Id} | tenant={TenantId}
///
/// Nivel de log:
///   - 2xx, 3xx → Information
///   - 4xx       → Warning  (error del cliente, no necesariamente un problema del servidor)
///   - 5xx       → Error    (ya logueado con detalle en ExceptionHandlerMiddleware)
/// </summary>
public class RequestLoggingMiddleware(RequestDelegate next, ILogger<RequestLoggingMiddleware> logger)
{
    public async Task InvokeAsync(HttpContext context)
    {
        var sw = Stopwatch.StartNew();

        await next(context);

        sw.Stop();

        var correlationId = context.Items["CorrelationId"]?.ToString() ?? "-";
        var tenantId = context.User?.FindFirst("tenant_id")?.Value ?? "público";
        var statusCode = context.Response.StatusCode;

        var nivel = statusCode switch
        {
            >= 500 => LogLevel.Error,
            >= 400 => LogLevel.Warning,
            _      => LogLevel.Information
        };

        logger.Log(
            nivel,
            "HTTP {Method} {Path} → {StatusCode} en {ElapsedMs}ms | correlationId={CorrelationId} | tenant={TenantId}",
            context.Request.Method,
            context.Request.Path,
            statusCode,
            sw.ElapsedMilliseconds,
            correlationId,
            tenantId
        );
    }
}
