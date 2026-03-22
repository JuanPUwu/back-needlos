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

    /// <summary>Lista todas las órdenes de la sastrería.</summary>
    /// <remarks>Devuelve las órdenes más recientes primero. Cada orden incluye cliente, estado, precio total y detalles.</remarks>
    /// <param name="pagina">Número de página. Empieza en 1.</param>
    /// <param name="tamano">Cantidad de órdenes por página. Máximo 100, por defecto 20.</param>
    /// <response code="200">Lista paginada de órdenes con el total de registros y páginas.</response>
    /// <response code="400">Los parámetros de paginación son inválidos.</response>
    [HttpGet]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> Obtener([FromQuery] int pagina = 1, [FromQuery] int tamano = 20)
    {
        var resultado = await _mediator.Send(new ObtenerOrdenesQuery(pagina, tamano));
        return Ok(resultado);
    }

    /// <summary>Obtiene el detalle completo de una orden.</summary>
    /// <remarks>Incluye cliente, estado actual, precio total, servicios incluidos y pagos registrados.</remarks>
    /// <response code="200">Detalle completo de la orden.</response>
    /// <response code="404">No existe ninguna orden con ese id.</response>
    [HttpGet("{id}")]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> ObtenerPorId(Guid id)
    {
        var orden = await _mediator.Send(new ObtenerOrdenPorIdQuery(id));
        return Ok(orden);
    }

    /// <summary>Crea una nueva orden para un cliente.</summary>
    /// <remarks>
    /// La orden debe tener al menos un servicio. El precio de cada servicio se especifica manualmente.
    /// La fecha de entrega debe ser futura. El estado inicial es siempre "Pendiente".
    /// </remarks>
    /// <response code="201">Orden creada correctamente. Devuelve el id de la nueva orden.</response>
    /// <response code="400">Los datos son inválidos (sin detalles, fecha pasada, precio negativo, etc.).</response>
    /// <response code="404">El cliente o alguno de los servicios indicados no existe.</response>
    [HttpPost]
    [ProducesResponseType(StatusCodes.Status201Created)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Crear([FromBody] CrearOrdenCommand command)
    {
        var id = await _mediator.Send(command);
        return CreatedAtAction(nameof(ObtenerPorId), new { id }, new { id });
    }

    /// <summary>Consulta el historial de estados de una orden.</summary>
    /// <remarks>Muestra cada cambio de estado en orden cronológico: quién lo hizo, cuándo y desde qué estado pasó a cuál.</remarks>
    /// <response code="200">Lista cronológica de cambios de estado.</response>
    /// <response code="404">No existe ninguna orden con ese id.</response>
    [HttpGet("{id}/historial")]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> ObtenerHistorial(Guid id)
    {
        var historial = await _mediator.Send(new ObtenerHistorialOrdenQuery(id));
        return Ok(historial);
    }

    /// <summary>Cambia el estado de una orden.</summary>
    /// <remarks>
    /// Estados posibles: 0 = Pendiente, 1 = En Proceso, 2 = Listo, 3 = Entregado.
    /// Una orden marcada como "Entregado" no puede cambiar de estado.
    /// </remarks>
    /// <response code="204">Estado actualizado correctamente.</response>
    /// <response code="400">Cambio de estado no permitido (por ejemplo, la orden ya fue entregada).</response>
    /// <response code="404">No existe ninguna orden con ese id.</response>
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
