using MediatR;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Needlos.Aplicacion.TiposPrendas.Consultas.ObtenerTiposPrendas;

namespace Needlos.Api.Controllers;

[ApiController]
[Route("api/tipos-prendas")]
[Authorize(Roles = "Admin,SuperAdmin")]
public class TipoPrendasController : ControllerBase
{
    private readonly IMediator _mediator;

    public TipoPrendasController(IMediator mediator)
    {
        _mediator = mediator;
    }

    /// <summary>Lista todos los tipos de prenda disponibles.</summary>
    /// <remarks>Catálogo global predefinido. Se usa al crear una orden para seleccionar el tipo de cada prenda.</remarks>
    /// <response code="200">Lista de tipos de prenda ordenados alfabéticamente.</response>
    [HttpGet]
    [ProducesResponseType(StatusCodes.Status200OK)]
    public async Task<IActionResult> Obtener()
    {
        var resultado = await _mediator.Send(new ObtenerTiposPrendasQuery());
        return Ok(resultado);
    }
}
