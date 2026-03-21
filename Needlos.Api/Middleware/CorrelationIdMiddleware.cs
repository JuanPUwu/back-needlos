namespace Needlos.Api.Middleware;

/// <summary>
/// Middleware que gestiona el CorrelationId de cada request.
///
/// El CorrelationId permite trazar una petición a través de todos los logs,
/// incluso si genera múltiples operaciones internas. Es fundamental en producción
/// para debuggear errores y correlacionar entradas de log.
///
/// Comportamiento:
///   - Si el cliente envía el header "X-Correlation-Id", se respeta ese valor.
///   - Si no, se genera un GUID nuevo.
///   - El valor se incluye siempre en el header de respuesta "X-Correlation-Id".
///   - Se almacena en HttpContext.Items["CorrelationId"] para que otros
///     middleware y handlers puedan leerlo (especialmente el logger de errores).
/// </summary>
public class CorrelationIdMiddleware(RequestDelegate next)
{
    private const string HeaderName = "X-Correlation-Id";

    public async Task InvokeAsync(HttpContext context)
    {
        var correlationId = context.Request.Headers[HeaderName].FirstOrDefault()
            ?? Guid.NewGuid().ToString();

        context.Items["CorrelationId"] = correlationId;
        context.Response.Headers[HeaderName] = correlationId;

        await next(context);
    }
}
