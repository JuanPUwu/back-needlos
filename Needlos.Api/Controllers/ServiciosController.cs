using MediatR;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Needlos.Aplicacion.Servicios.Comandos.ActualizarServicio;
using Needlos.Aplicacion.Servicios.Comandos.CrearServicio;
using Needlos.Aplicacion.Servicios.Comandos.EliminarServicio;
using Needlos.Aplicacion.Servicios.Consultas.ObtenerServicioPorId;
using Needlos.Aplicacion.Servicios.Consultas.ObtenerServicios;

namespace Needlos.Api.Controllers;

[ApiController]
[Route("api/servicios")]
[Authorize(Roles = "Admin,SuperAdmin")]
public class ServiciosController : ControllerBase
{
    private readonly IMediator _mediator;

    public ServiciosController(IMediator mediator)
    {
        _mediator = mediator;
    }

    /// <summary>Lista todos los servicios que ofrece la sastrería.</summary>
    /// <remarks>Devuelve los servicios ordenados alfabéticamente por nombre. Estos son los servicios disponibles para agregar a las órdenes.</remarks>
    /// <param name="pagina">Número de página. Empieza en 1.</param>
    /// <param name="tamano">Cantidad de servicios por página. Máximo 100, por defecto 20.</param>
    /// <response code="200">Lista paginada de servicios con el total de registros y páginas.</response>
    /// <response code="400">Los parámetros de paginación son inválidos.</response>
    [HttpGet]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> Obtener([FromQuery] int pagina = 1, [FromQuery] int tamano = 20)
    {
        var resultado = await _mediator.Send(new ObtenerServiciosQuery(pagina, tamano));
        return Ok(resultado);
    }

    /// <summary>Obtiene el detalle de un servicio.</summary>
    /// <response code="200">Datos del servicio: nombre y precio base.</response>
    /// <response code="404">No existe ningún servicio con ese id.</response>
    [HttpGet("{id}")]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> ObtenerPorId(Guid id)
    {
        var servicio = await _mediator.Send(new ObtenerServicioPorIdQuery(id));
        return Ok(servicio);
    }

    /// <summary>Agrega un nuevo servicio al catálogo de la sastrería.</summary>
    /// <remarks>El precio base sirve como referencia; al crear una orden se puede especificar un precio diferente por servicio.</remarks>
    /// <response code="201">Servicio creado correctamente. Devuelve el id asignado.</response>
    /// <response code="400">Los datos son inválidos (nombre vacío o precio negativo).</response>
    [HttpPost]
    [ProducesResponseType(StatusCodes.Status201Created)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> Crear([FromBody] CrearServicioCommand command)
    {
        var id = await _mediator.Send(command);
        return StatusCode(StatusCodes.Status201Created, new { id });
    }

    /// <summary>Actualiza el nombre o precio de un servicio.</summary>
    /// <response code="204">Servicio actualizado correctamente.</response>
    /// <response code="400">Los datos son inválidos.</response>
    /// <response code="404">No existe ningún servicio con ese id.</response>
    [HttpPut("{id}")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Actualizar(Guid id, [FromBody] ActualizarServicioCommand command)
    {
        await _mediator.Send(command with { Id = id });
        return NoContent();
    }

    /// <summary>Elimina un servicio del catálogo.</summary>
    /// <remarks>El servicio no se borra físicamente. Las órdenes existentes que lo usaban no se ven afectadas.</remarks>
    /// <response code="204">Servicio eliminado correctamente.</response>
    /// <response code="404">No existe ningún servicio con ese id.</response>
    [HttpDelete("{id}")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Eliminar(Guid id)
    {
        await _mediator.Send(new EliminarServicioCommand(id));
        return NoContent();
    }
}
