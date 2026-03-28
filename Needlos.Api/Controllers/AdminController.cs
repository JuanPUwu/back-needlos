using MediatR;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Needlos.Aplicacion.Admin.Comandos.ConfigurarSuperAdmin;
using Needlos.Aplicacion.Admin.Comandos.LimpiarTokensExpirados;
using Needlos.Aplicacion.Admin.Consultas.ObtenerEstadisticasBd;
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
    /// Solo accesible para SuperAdmin. Crea un nuevo usuario con rol SuperAdmin sin límite de cantidad.
    /// El SuperAdmin tiene acceso global a todos los tenants del sistema.
    /// </remarks>
    /// <response code="201">SuperAdmin creado. Devuelve el id del nuevo usuario.</response>
    /// <response code="400">Los datos enviados no son válidos.</response>
    /// <response code="401">No autenticado.</response>
    /// <response code="403">No tiene permisos de SuperAdmin.</response>
    /// <response code="409">El email ya está registrado.</response>
    [HttpPost("superadmins")]
    [ProducesResponseType(StatusCodes.Status201Created)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(StatusCodes.Status403Forbidden)]
    [ProducesResponseType(StatusCodes.Status409Conflict)]
    public async Task<IActionResult> CrearSuperAdmin([FromBody] ConfigurarSuperAdminCommand command)
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

    /// <summary>Elimina manualmente los refresh tokens expirados de la base de datos.</summary>
    /// <remarks>
    /// La limpieza también se ejecuta automáticamente cada 24 horas al arrancar el servidor.
    /// Solo elimina tokens cuya fecha de expiración ya pasó — los tokens usados pero aún
    /// vigentes se conservan durante su ventana de 7 días (útil para detectar robo de tokens).
    /// </remarks>
    /// <response code="200">Limpieza completada. Devuelve la cantidad de tokens eliminados.</response>
    [HttpPost("mantenimiento/limpiar-tokens")]
    [ProducesResponseType(StatusCodes.Status200OK)]
    public async Task<IActionResult> LimpiarTokensExpirados()
    {
        var eliminados = await _mediator.Send(new LimpiarTokensExpiradosCommand());
        return Ok(new { eliminados });
    }

    /// <summary>Devuelve el tamaño actual de la base de datos desglosado por tabla.</summary>
    /// <remarks>
    /// Útil para monitorear el crecimiento de la BD desde el panel de administración.
    /// Muestra para cada tabla: filas estimadas, tamaño de datos, tamaño de índices y tamaño total.
    /// Los tamaños se incluyen en bytes (para cálculos) y en formato legible (ej: "1.2 MB").
    /// </remarks>
    /// <response code="200">Estadísticas de tamaño de la BD.</response>
    [HttpGet("estadisticas/bd")]
    [ProducesResponseType(StatusCodes.Status200OK)]
    public async Task<IActionResult> ObtenerEstadisticasBd()
    {
        var resultado = await _mediator.Send(new ObtenerEstadisticasBdQuery());
        return Ok(resultado);
    }
}
