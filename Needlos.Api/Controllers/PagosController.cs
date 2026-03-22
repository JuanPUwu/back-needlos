using MediatR;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Needlos.Aplicacion.Pagos.Comandos.CrearPago;
using Needlos.Aplicacion.Pagos.Consultas.ObtenerPagosPorOrden;

namespace Needlos.Api.Controllers;

[ApiController]
[Route("api/pagos")]
[Authorize(Roles = "Admin,SuperAdmin")]
public class PagosController : ControllerBase
{
    private readonly IMediator _mediator;

    public PagosController(IMediator mediator)
    {
        _mediator = mediator;
    }

    /// <summary>Devuelve los pagos de una orden.</summary>
    /// <response code="200">Lista de pagos de la orden.</response>
    /// <response code="404">La ordenId no existe.</response>
    [HttpGet]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> ObtenerPorOrden([FromQuery] Guid ordenId)
    {
        var pagos = await _mediator.Send(new ObtenerPagosPorOrdenQuery(ordenId));
        return Ok(pagos);
    }

    /// <summary>Registra un pago para una orden existente.</summary>
    /// <response code="201">Pago registrado. Devuelve el id.</response>
    /// <response code="400">Datos inválidos.</response>
    /// <response code="404">La ordenId no existe.</response>
    [HttpPost]
    [ProducesResponseType(StatusCodes.Status201Created)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Crear([FromBody] CrearPagoCommand command)
    {
        var id = await _mediator.Send(command);
        return StatusCode(StatusCodes.Status201Created, new { id });
    }
}
