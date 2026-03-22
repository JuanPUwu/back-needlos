using MediatR;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Needlos.Aplicacion.Ordenes.Comandos.ActualizarEstadoOrden;
using Needlos.Aplicacion.Ordenes.Comandos.CrearOrden;
using Needlos.Aplicacion.Ordenes.Consultas.ObtenerHistorialOrden;
using Needlos.Aplicacion.Ordenes.Consultas.ObtenerOrdenPorId;
using Needlos.Aplicacion.Ordenes.Consultas.ObtenerOrdenes;
using Needlos.Dominio.Enumeraciones;

namespace Needlos.Api.Controllers;

[ApiController]
[Route("api/ordenes")]
[Authorize(Roles = "Admin,SuperAdmin")]
public class OrdenesController : ControllerBase
{
    private readonly IMediator _mediator;

    public OrdenesController(IMediator mediator)
    {
        _mediator = mediator;
    }

    /// <summary>Devuelve las órdenes del tenant paginadas.</summary>
    /// <param name="pagina">Número de página (mínimo 1, default 1).</param>
    /// <param name="tamano">Elementos por página (1-100, default 20).</param>
    /// <response code="200">Resultado paginado: datos, pagina, tamano, total, totalPaginas.</response>
    /// <response code="400">Parámetros de paginación inválidos.</response>
    [HttpGet]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> Obtener([FromQuery] int pagina = 1, [FromQuery] int tamano = 20)
    {
        var resultado = await _mediator.Send(new ObtenerOrdenesQuery(pagina, tamano));
        return Ok(resultado);
    }

    /// <summary>Devuelve una orden por su Id.</summary>
    /// <response code="200">Orden encontrada.</response>
    /// <response code="404">Orden no encontrada.</response>
    [HttpGet("{id}")]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> ObtenerPorId(Guid id)
    {
        var orden = await _mediator.Send(new ObtenerOrdenPorIdQuery(id));
        return Ok(orden);
    }

    /// <summary>Crea una nueva orden para un cliente existente.</summary>
    /// <response code="201">Orden creada. Devuelve el id.</response>
    /// <response code="400">Datos inválidos.</response>
    /// <response code="404">El clienteId no existe.</response>
    [HttpPost]
    [ProducesResponseType(StatusCodes.Status201Created)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Crear([FromBody] CrearOrdenCommand command)
    {
        var id = await _mediator.Send(command);
        return CreatedAtAction(nameof(ObtenerPorId), new { id }, new { id });
    }

    /// <summary>Devuelve el historial de cambios de estado de una orden.</summary>
    /// <response code="200">Lista de transiciones de estado ordenada cronológicamente.</response>
    /// <response code="404">Orden no encontrada.</response>
    [HttpGet("{id}/historial")]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> ObtenerHistorial(Guid id)
    {
        var historial = await _mediator.Send(new ObtenerHistorialOrdenQuery(id));
        return Ok(historial);
    }

    /// <summary>Actualiza el estado de una orden existente.</summary>
    /// <response code="204">Estado actualizado.</response>
    /// <response code="400">Transición de estado inválida (ej: orden ya entregada).</response>
    /// <response code="404">Orden no encontrada.</response>
    [HttpPut("{id}/estado")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> ActualizarEstado(Guid id, [FromBody] EstadoOrden nuevoEstado)
    {
        await _mediator.Send(new ActualizarEstadoOrdenCommand(id, nuevoEstado));
        return NoContent();
    }
}
