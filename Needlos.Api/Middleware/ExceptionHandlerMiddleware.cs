using System.Text.Json;
using Needlos.Aplicacion.Excepciones;
using Needlos.Dominio.Excepciones;
using ValidationException = Needlos.Aplicacion.Excepciones.ValidationException;

namespace Needlos.Api.Middleware;

/// <summary>
/// Middleware global que intercepta todas las excepciones no controladas y las convierte
/// en respuestas JSON con el status code HTTP correcto.
///
/// Formato de respuesta para todos los errores:
///   { "mensaje": "descripción", "errores": ["detalle1", "detalle2"] }
///
/// El campo "errores" solo aparece en errores de validación (400 de FluentValidation).
/// Para el resto, "errores" no se incluye.
///
/// Mapa de excepciones → HTTP:
///   ValidationException         → 400  (errores de FluentValidation)
///   BusinessException           → 400  (regla de negocio violada en el dominio)
///   NotFoundException           → 404  (recurso no encontrado)
///   ConflictException           → 409  (recurso duplicado)
///   UnauthorizedAccessException → 401  (credenciales inválidas)
///   Exception (cualquier otra)  → 500  (el mensaje real NO se expone al cliente)
/// </summary>
public class ExceptionHandlerMiddleware(RequestDelegate next, ILogger<ExceptionHandlerMiddleware> logger)
{
    private static readonly JsonSerializerOptions _jsonOptions = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase
    };

    public async Task InvokeAsync(HttpContext context)
    {
        try
        {
            await next(context);
        }
        catch (Exception ex)
        {
            await ManejarExcepcion(context, ex);
        }
    }

    private async Task ManejarExcepcion(HttpContext context, Exception ex)
    {
        context.Response.ContentType = "application/json";

        object respuesta;

        switch (ex)
        {
            case ValidationException validacion:
                context.Response.StatusCode = StatusCodes.Status400BadRequest;
                respuesta = new
                {
                    mensaje = validacion.Message,
                    errores = validacion.Errores
                };
                break;

            case BusinessException negocio:
                context.Response.StatusCode = StatusCodes.Status400BadRequest;
                respuesta = new { mensaje = negocio.Message };
                break;

            case NotFoundException notFound:
                context.Response.StatusCode = StatusCodes.Status404NotFound;
                respuesta = new { mensaje = notFound.Message };
                break;

            case ConflictException conflict:
                context.Response.StatusCode = StatusCodes.Status409Conflict;
                respuesta = new { mensaje = conflict.Message };
                break;

            case UnauthorizedAccessException unauthorized:
                context.Response.StatusCode = StatusCodes.Status401Unauthorized;
                respuesta = new { mensaje = unauthorized.Message };
                break;

            default:
                var correlationId = context.Items["CorrelationId"]?.ToString() ?? "-";
                var tenantId = context.User?.FindFirst("tenant_id")?.Value ?? "público";
                logger.LogError(
                    ex,
                    "Error no controlado | correlationId={CorrelationId} | tenant={TenantId} | path={Path}",
                    correlationId,
                    tenantId,
                    context.Request.Path
                );
                context.Response.StatusCode = StatusCodes.Status500InternalServerError;
                respuesta = new { mensaje = "Ha ocurrido un error inesperado." };
                break;
        }

        await context.Response.WriteAsync(JsonSerializer.Serialize(respuesta, _jsonOptions));
    }
}
