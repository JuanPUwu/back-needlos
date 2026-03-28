using System.Security.Claims;
using Microsoft.AspNetCore.Http;
using Needlos.Aplicacion.Contratos;

namespace Needlos.Infraestructura.Tenancy;

public class TenantProvider : ITenantProvider
{
    private readonly IHttpContextAccessor _httpContextAccessor;

    public TenantProvider(IHttpContextAccessor httpContextAccessor)
    {
        _httpContextAccessor = httpContextAccessor;
    }

    public Guid GetTenantId()
    {
        var claim = _httpContextAccessor.HttpContext?.User?.FindFirst("tenant_id");

        if (claim != null && Guid.TryParse(claim.Value, out var tenantId))
            return tenantId;

        return Guid.Empty;
    }

    public Guid GetUsuarioId()
    {
        // El middleware JWT mapea automáticamente "sub" → ClaimTypes.NameIdentifier
        var claim = _httpContextAccessor.HttpContext?.User?.FindFirst(ClaimTypes.NameIdentifier);

        if (claim != null && Guid.TryParse(claim.Value, out var usuarioId))
            return usuarioId;

        return Guid.Empty;
    }
}
