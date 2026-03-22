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

    /// <summary>Crea un nuevo SuperAdmin del sistema.</summary>
    /// <remarks>
    /// Endpoint público. Solo se puede usar hasta 2 veces — el sistema admite máximo 2 SuperAdmins.
    /// El SuperAdmin tiene acceso global a todos los tenants del sistema.
    /// </remarks>
    /// <response code="201">SuperAdmin creado. Devuelve el id del nuevo usuario.</response>
    /// <response code="400">Los datos enviados no son válidos.</response>
    /// <response code="409">Ya existen 2 SuperAdmins configurados. No se pueden crear más.</response>
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

    /// <summary>Lista todas las sastrerías registradas en el sistema.</summary>
    /// <remarks>Solo accesible para SuperAdmin. Devuelve id, nombre, slug, estado y fecha de registro.</remarks>
    /// <response code="200">Lista paginada de tenants. Incluye total de páginas para la navegación.</response>
    /// <response code="400">Los parámetros de paginación son inválidos (pagina o tamano fuera de rango).</response>
    [HttpGet("tenants")]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> ObtenerTenants(
        [FromQuery] int pagina = 1, [FromQuery] int tamano = 20)
    {
        var resultado = await _mediator.Send(new ObtenerTenantsQuery(pagina, tamano));
        return Ok(resultado);
    }

    /// <summary>Lista los usuarios de una sastrería específica.</summary>
    /// <remarks>Solo accesible para SuperAdmin. Muestra email, estado y roles de cada usuario.</remarks>
    /// <response code="200">Lista paginada de usuarios con sus roles asignados.</response>
    /// <response code="400">Los parámetros de paginación son inválidos.</response>
    /// <response code="404">No existe ninguna sastrería con ese tenantId.</response>
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
