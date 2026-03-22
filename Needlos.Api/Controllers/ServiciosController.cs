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

    /// <summary>Devuelve los servicios del tenant paginados.</summary>
    /// <param name="pagina">Número de página (mínimo 1, default 1).</param>
    /// <param name="tamano">Elementos por página (1-100, default 20).</param>
    /// <response code="200">Resultado paginado: datos, pagina, tamano, total, totalPaginas.</response>
    /// <response code="400">Parámetros de paginación inválidos.</response>
    [HttpGet]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> Obtener([FromQuery] int pagina = 1, [FromQuery] int tamano = 20)
    {
        var resultado = await _mediator.Send(new ObtenerServiciosQuery(pagina, tamano));
        return Ok(resultado);
    }

    /// <summary>Devuelve un servicio por su Id.</summary>
    /// <response code="200">Servicio encontrado.</response>
    /// <response code="404">Servicio no encontrado.</response>
    [HttpGet("{id}")]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> ObtenerPorId(Guid id)
    {
        var servicio = await _mediator.Send(new ObtenerServicioPorIdQuery(id));
        return Ok(servicio);
    }

    /// <summary>Crea un nuevo servicio.</summary>
    /// <response code="201">Servicio creado. Devuelve el id.</response>
    /// <response code="400">Datos inválidos.</response>
    [HttpPost]
    [ProducesResponseType(StatusCodes.Status201Created)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> Crear([FromBody] CrearServicioCommand command)
    {
        var id = await _mediator.Send(command);
        return StatusCode(StatusCodes.Status201Created, new { id });
    }

    /// <summary>Actualiza los datos de un servicio existente.</summary>
    /// <response code="204">Servicio actualizado.</response>
    /// <response code="400">Datos inválidos.</response>
    /// <response code="404">Servicio no encontrado.</response>
    [HttpPut("{id}")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Actualizar(Guid id, [FromBody] ActualizarServicioCommand command)
    {
        await _mediator.Send(command with { Id = id });
        return NoContent();
    }

    /// <summary>Elimina (soft delete) un servicio.</summary>
    /// <response code="204">Servicio eliminado.</response>
    /// <response code="404">Servicio no encontrado.</response>
    [HttpDelete("{id}")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Eliminar(Guid id)
    {
        await _mediator.Send(new EliminarServicioCommand(id));
        return NoContent();
    }
}
