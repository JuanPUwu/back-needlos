using Microsoft.AspNetCore.Authorization;
using Microsoft.OpenApi;
using Swashbuckle.AspNetCore.SwaggerGen;

namespace Needlos.Api.Swagger;

/// <summary>
/// Agrega el candado y el security requirement de Bearer a cada operación
/// que tenga [Authorize], garantizando que Swagger UI envíe el header Authorization.
/// </summary>
public class BearerAuthOperationFilter : IOperationFilter
{
    public void Apply(OpenApiOperation operation, OperationFilterContext context)
    {
        var tieneAuthorize =
            context.MethodInfo.DeclaringType!.GetCustomAttributes(true).OfType<AuthorizeAttribute>().Any() ||
            context.MethodInfo.GetCustomAttributes(true).OfType<AuthorizeAttribute>().Any();

        if (!tieneAuthorize) return;

        // Pasar context.Document resuelve el Target del esquema Bearer,
        // lo que permite que el serializador emita {"Bearer": []} en lugar de {}
        var bearerRef = new OpenApiSecuritySchemeReference("Bearer", context.Document);
        operation.Security ??= [];
        operation.Security.Add(new OpenApiSecurityRequirement { { bearerRef, [] } });
    }
}
