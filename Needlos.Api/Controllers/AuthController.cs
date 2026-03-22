using MediatR;
using Microsoft.AspNetCore.Mvc;
using Needlos.Aplicacion.Auth.Comandos.Login;
using Needlos.Aplicacion.Auth.Comandos.Registrar;

namespace Needlos.Api.Controllers;

[ApiController]
[Route("api/auth")]
public class AuthController : ControllerBase
{
    private readonly IMediator _mediator;

    public AuthController(IMediator mediator)
    {
        _mediator = mediator;
    }

    /// <summary>Registra una nueva sastrería en el sistema.</summary>
    /// <remarks>
    /// Crea el tenant (sastrería) y su usuario administrador en un solo paso.
    /// El email debe ser único. La contraseña requiere mínimo 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial.
    /// El teléfono debe tener al menos 10 dígitos.
    /// </remarks>
    /// <response code="201">Sastrería creada correctamente. Devuelve el tenantId asignado.</response>
    /// <response code="400">Los datos enviados no son válidos (email incorrecto, contraseña débil, etc.).</response>
    /// <response code="409">Ya existe una cuenta con ese email.</response>
    [HttpPost("registrar")]
    [ProducesResponseType(StatusCodes.Status201Created)]
    [ProducesResponseType(StatusCodes.Status409Conflict)]
    public async Task<IActionResult> Registrar([FromBody] RegistrarTenantCommand command)
    {
        var tenantId = await _mediator.Send(command);
        return StatusCode(StatusCodes.Status201Created, new { tenantId });
    }

    /// <summary>Inicia sesión y obtiene el token de acceso.</summary>
    /// <remarks>
    /// Devuelve un JWT que debes incluir en el header Authorization de todas las demás peticiones.
    /// El token expira en 24 horas. La cuenta de sistema usa email: "admin" / password: "admin".
    /// </remarks>
    /// <response code="200">Login exitoso. Devuelve el token JWT, el tenantId y el email del usuario.</response>
    /// <response code="401">Email o contraseña incorrectos, o el usuario está desactivado.</response>
    [HttpPost("login")]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status401Unauthorized)]
    public async Task<IActionResult> Login([FromBody] LoginCommand command)
    {
        var resultado = await _mediator.Send(command);
        return Ok(resultado);
    }
}
