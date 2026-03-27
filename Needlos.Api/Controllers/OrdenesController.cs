using MediatR;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Needlos.Aplicacion.Ordenes.Comandos.CambiarEstadoPrenda;
using Needlos.Aplicacion.Ordenes.Comandos.CrearOrden;
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
    /// <remarks>
    /// Devuelve las órdenes más recientes primero.
    /// El estado y la fecha de entrega de cada orden se calculan automáticamente a partir de sus prendas:
    /// el estado es el más joven entre todas las prendas y la fecha es la más próxima.
    /// </remarks>
    /// <param name="pagina">Número de página. Empieza en 1.</param>
    /// <param name="tamano">Cantidad de órdenes por página. Máximo 100, por defecto 20.</param>
    /// <response code="200">Lista paginada de órdenes con prendas, estado y precio total.</response>
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
    /// <remarks>Incluye cliente, tipo, todas las prendas con sus estados individuales, precio total y fecha de entrega.</remarks>
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

    /// <summary>Crea una nueva orden para un cliente existente.</summary>
    /// <remarks>
    /// Cada prenda debe indicar: tipo de prenda, cantidad, descripción del trabajo, precio por unidad y fecha de entrega estimada.
    /// El tipo de orden puede ser 0=Arreglo (por defecto) o 1=Confección.
    /// La fecha de entrega de la orden se calcula automáticamente como la fecha más próxima entre todas las prendas.
    /// </remarks>
    /// <response code="201">Orden creada correctamente. Devuelve el id de la nueva orden.</response>
    /// <response code="400">Los datos son inválidos (sin prendas, fecha pasada, precio negativo, etc.).</response>
    /// <response code="404">El cliente o algún tipo de prenda indicado no existe.</response>
    [HttpPost]
    [ProducesResponseType(StatusCodes.Status201Created)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Crear([FromBody] CrearOrdenCommand command)
    {
        var id = await _mediator.Send(command);
        return CreatedAtAction(nameof(ObtenerPorId), new { id }, new { id });
    }

    /// <summary>Cambia el estado de una prenda específica dentro de una orden.</summary>
    /// <remarks>
    /// Estados posibles: 0=EnProceso, 1=Finalizado, 2=Entregado.
    /// Una prenda marcada como Entregada no puede cambiar de estado.
    /// El estado global de la orden se recalcula automáticamente al consultar.
    /// </remarks>
    /// <response code="204">Estado de la prenda actualizado correctamente.</response>
    /// <response code="400">Cambio de estado no permitido (la prenda ya fue entregada) o estado inválido.</response>
    /// <response code="404">La orden o la prenda indicada no existe.</response>
    [HttpPut("{id}/prendas/{prendaId}/estado")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> CambiarEstadoPrenda(Guid id, Guid prendaId, [FromBody] EstadoPrenda nuevoEstado)
    {
        await _mediator.Send(new CambiarEstadoPrendaCommand(id, prendaId, nuevoEstado));
        return NoContent();
    }
}
