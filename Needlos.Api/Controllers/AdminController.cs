using MediatR;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Needlos.Aplicacion.Admin.Comandos.ConfigurarSuperAdmin;
using Needlos.Aplicacion.Admin.Consultas.ObtenerTenants;
using Needlos.Aplicacion.Admin.Consultas.ObtenerUsuariosPorTenant;

namespace Needlos.Api.Controllers;

[ApiController]
[Route("api/admin")]
[Authorize(Roles = "SuperAdmin")]
public class AdminController : ControllerBase
{
    private readonly IMediator _mediator;

    public AdminController(IMediator mediator)
    {
        _mediator = mediator;
    }

    /// <summary>
    /// Configura el usuario SuperAdmin del sistema. Solo puede ejecutarse una vez.
    /// Si ya existe un SuperAdmin, retorna 409 Conflict.
    /// </summary>
    /// <response code="201">SuperAdmin creado. Devuelve el id del usuario.</response>
    /// <response code="400">Datos inválidos.</response>
    /// <response code="409">El SuperAdmin ya está configurado.</response>
    [HttpPost("setup")]
    [AllowAnonymous]
    [ProducesResponseType(StatusCodes.Status201Created)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status409Conflict)]
    public async Task<IActionResult> Setup([FromBody] ConfigurarSuperAdminCommand command)
    {
        var id = await _mediator.Send(command);
        return StatusCode(StatusCodes.Status201Created, new { id });
    }

    /// <summary>Devuelve todos los tenants del sistema paginados.</summary>
    /// <response code="200">Lista paginada de tenants.</response>
    /// <response code="400">Parámetros de paginación inválidos.</response>
    [HttpGet("tenants")]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> ObtenerTenants(
        [FromQuery] int pagina = 1, [FromQuery] int tamano = 20)
    {
        var resultado = await _mediator.Send(new ObtenerTenantsQuery(pagina, tamano));
        return Ok(resultado);
    }

    /// <summary>Devuelve los usuarios de un tenant específico paginados.</summary>
    /// <response code="200">Lista paginada de usuarios del tenant.</response>
    /// <response code="400">Parámetros inválidos.</response>
    /// <response code="404">Tenant no encontrado.</response>
    [HttpGet("tenants/{tenantId}/usuarios")]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> ObtenerUsuarios(
        Guid tenantId, [FromQuery] int pagina = 1, [FromQuery] int tamano = 20)
    {
        var resultado = await _mediator.Send(new ObtenerUsuariosPorTenantQuery(tenantId, pagina, tamano));
        return Ok(resultado);
    }
}
