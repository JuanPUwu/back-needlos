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

    /// <summary>Lista todos los pagos registrados para una orden.</summary>
    /// <remarks>Devuelve los pagos ordenados por fecha. Útil para ver cuánto se ha abonado y qué método se usó.</remarks>
    /// <response code="200">Lista de pagos con monto, método y fecha de cada uno.</response>
    /// <response code="404">No existe ninguna orden con ese ordenId.</response>
    [HttpGet]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> ObtenerPorOrden([FromQuery] Guid ordenId)
    {
        var pagos = await _mediator.Send(new ObtenerPagosPorOrdenQuery(ordenId));
        return Ok(pagos);
    }

    /// <summary>Registra un pago sobre una orden.</summary>
    /// <remarks>
    /// Se pueden registrar múltiples pagos sobre la misma orden (abonos parciales).
    /// Métodos de pago: 0 = Efectivo, 1 = Transferencia, 2 = Tarjeta.
    /// </remarks>
    /// <response code="201">Pago registrado correctamente. Devuelve el id del pago.</response>
    /// <response code="400">Los datos son inválidos (monto negativo, método inválido, etc.).</response>
    /// <response code="404">No existe ninguna orden con ese ordenId.</response>
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
