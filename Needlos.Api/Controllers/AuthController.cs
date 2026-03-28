using MediatR;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;
using Needlos.Aplicacion.Auth.Comandos.Login;
using Needlos.Aplicacion.Auth.Comandos.Logout;
using Needlos.Aplicacion.Auth.Comandos.Refresh;
using Needlos.Aplicacion.Auth.Comandos.Registrar;

namespace Needlos.Api.Controllers;

[ApiController]
[Route("api/auth")]
public class AuthController : ControllerBase
{
    private readonly IMediator _mediator;
    private readonly IConfiguration _configuration;

    public AuthController(IMediator mediator, IConfiguration configuration)
    {
        _mediator = mediator;
        _configuration = configuration;
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

    /// <summary>Inicia sesión y obtiene los tokens de acceso.</summary>
    /// <remarks>
    /// Devuelve el access token en el body (expira en 10 min) y establece el refresh token
    /// en una cookie HttpOnly (expira en 7 días). El access token debe incluirse en el header
    /// Authorization de todas las peticiones posteriores.
    /// </remarks>
    /// <response code="200">Login exitoso. Devuelve accessToken, tenantId y email.</response>
    /// <response code="401">Email o contraseña incorrectos, o el usuario está desactivado.</response>
    /// <response code="429">Demasiados intentos de login. Espera antes de volver a intentar.</response>
    [HttpPost("login")]
    [EnableRateLimiting("auth")]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(StatusCodes.Status429TooManyRequests)]
    public async Task<IActionResult> Login([FromBody] LoginCommand command)
    {
        var resultado = await _mediator.Send(command);

        Response.Cookies.Append("refreshToken", resultado.RefreshTokenRaw, CookieOpciones());

        return Ok(new
        {
            accessToken = resultado.AccessToken,
            tenantId    = resultado.TenantId,
            email       = resultado.Email
        });
    }

    /// <summary>Renueva el access token usando el refresh token de la cookie.</summary>
    /// <remarks>
    /// El navegador envía automáticamente la cookie HttpOnly. Si el refresh token es válido,
    /// se rota (el anterior queda invalidado) y se emite un nuevo par de tokens.
    /// </remarks>
    /// <response code="200">Tokens renovados. Devuelve el nuevo accessToken.</response>
    /// <response code="401">No hay refresh token, o es inválido/expirado/ya usado.</response>
    [HttpPost("refresh")]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status401Unauthorized)]
    public async Task<IActionResult> Refresh()
    {
        var token = Request.Cookies["refreshToken"];
        if (string.IsNullOrEmpty(token))
            return Unauthorized(new { mensaje = "No hay refresh token." });

        var resultado = await _mediator.Send(new RefrescarTokenCommand(token));

        Response.Cookies.Append("refreshToken", resultado.RefreshTokenRaw, CookieOpciones());

        return Ok(new { accessToken = resultado.AccessToken });
    }

    /// <summary>Cierra la sesión del usuario actual.</summary>
    /// <remarks>
    /// Invalida el refresh token en base de datos y elimina la cookie.
    /// Es idempotente: si la cookie no existe o el token ya estaba invalidado, responde 204 igualmente.
    /// </remarks>
    /// <response code="204">Sesión cerrada correctamente.</response>
    [HttpPost("logout")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    public async Task<IActionResult> Logout()
    {
        var token = Request.Cookies["refreshToken"];
        if (!string.IsNullOrEmpty(token))
            await _mediator.Send(new CerrarSesionCommand(token));

        Response.Cookies.Delete("refreshToken");
        return NoContent();
    }

    private CookieOptions CookieOpciones()
    {
        var secure = _configuration.GetValue<bool>("Auth:CookieSegura");
        return new()
        {
            HttpOnly = true,
            Secure   = secure,
            // SameSite=None requiere Secure=true (exigido por los navegadores).
            // En desarrollo (Secure=false) se usa Lax: localhost:4200 → localhost:5193
            // es same-site, por lo que Lax envía la cookie en todos los métodos.
            SameSite = secure ? SameSiteMode.None : SameSiteMode.Lax,
            Expires  = DateTimeOffset.UtcNow.AddDays(7)
        };
    }
}
