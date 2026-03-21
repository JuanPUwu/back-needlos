using MediatR;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Needlos.Aplicacion.Clientes.Comandos.ActualizarCliente;
using Needlos.Aplicacion.Clientes.Comandos.CrearCliente;
using Needlos.Aplicacion.Clientes.Comandos.EliminarCliente;
using Needlos.Aplicacion.Clientes.Consultas.ObtenerClientePorId;
using Needlos.Aplicacion.Clientes.Consultas.ObtenerClientes;

namespace Needlos.Api.Controllers;

[ApiController]
[Route("api/clientes")]
[Authorize]
public class ClientesController : ControllerBase
{
    private readonly IMediator _mediator;

    public ClientesController(IMediator mediator)
    {
        _mediator = mediator;
    }

    /// <summary>Devuelve los clientes del tenant paginados.</summary>
    /// <param name="pagina">Número de página (mínimo 1, default 1).</param>
    /// <param name="tamano">Elementos por página (1-100, default 20).</param>
    /// <response code="200">Resultado paginado: datos, pagina, tamano, total, totalPaginas.</response>
    /// <response code="400">Parámetros de paginación inválidos.</response>
    [HttpGet]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> Obtener([FromQuery] int pagina = 1, [FromQuery] int tamano = 20)
    {
        var resultado = await _mediator.Send(new ObtenerClientesQuery(pagina, tamano));
        return Ok(resultado);
    }

    /// <summary>Devuelve un cliente por su Id.</summary>
    /// <response code="200">Cliente encontrado.</response>
    /// <response code="404">Cliente no encontrado.</response>
    [HttpGet("{id}")]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> ObtenerPorId(Guid id)
    {
        var cliente = await _mediator.Send(new ObtenerClientePorIdQuery(id));
        return Ok(cliente);
    }

    /// <summary>Crea un nuevo cliente.</summary>
    /// <response code="201">Cliente creado. Devuelve el id.</response>
    /// <response code="400">Datos inválidos.</response>
    [HttpPost]
    [ProducesResponseType(StatusCodes.Status201Created)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> Crear([FromBody] CrearClienteCommand command)
    {
        var id = await _mediator.Send(command);
        return StatusCode(StatusCodes.Status201Created, new { id });
    }

    /// <summary>Actualiza los datos de un cliente existente.</summary>
    /// <response code="204">Cliente actualizado.</response>
    /// <response code="400">Datos inválidos.</response>
    /// <response code="404">Cliente no encontrado.</response>
    [HttpPut("{id}")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Actualizar(Guid id, [FromBody] ActualizarClienteCommand command)
    {
        await _mediator.Send(command with { Id = id });
        return NoContent();
    }

    /// <summary>Elimina (soft delete) un cliente.</summary>
    /// <response code="204">Cliente eliminado.</response>
    /// <response code="404">Cliente no encontrado.</response>
    [HttpDelete("{id}")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Eliminar(Guid id)
    {
        await _mediator.Send(new EliminarClienteCommand(id));
        return NoContent();
    }
}
