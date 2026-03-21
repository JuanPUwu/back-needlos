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

    /// <summary>Registra un nuevo tenant con su usuario administrador.</summary>
    /// <response code="201">Tenant creado. Devuelve el tenantId.</response>
    /// <response code="409">El email ya está registrado.</response>
    [HttpPost("registrar")]
    [ProducesResponseType(StatusCodes.Status201Created)]
    [ProducesResponseType(StatusCodes.Status409Conflict)]
    public async Task<IActionResult> Registrar([FromBody] RegistrarTenantCommand command)
    {
        var tenantId = await _mediator.Send(command);
        return StatusCode(StatusCodes.Status201Created, new { tenantId });
    }

    /// <summary>Autentica un usuario y devuelve un JWT.</summary>
    /// <response code="200">Login exitoso. Devuelve token, tenantId y email.</response>
    /// <response code="401">Credenciales inválidas o usuario inactivo.</response>
    [HttpPost("login")]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status401Unauthorized)]
    public async Task<IActionResult> Login([FromBody] LoginCommand command)
    {
        var resultado = await _mediator.Send(command);
        return Ok(resultado);
    }
}
