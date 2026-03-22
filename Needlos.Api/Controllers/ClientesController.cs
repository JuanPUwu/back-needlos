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
[Authorize(Roles = "Admin,SuperAdmin")]
public class ClientesController : ControllerBase
{
    private readonly IMediator _mediator;

    public ClientesController(IMediator mediator)
    {
        _mediator = mediator;
    }

    /// <summary>Lista todos los clientes de la sastrería.</summary>
    /// <remarks>Devuelve los clientes ordenados alfabéticamente por nombre.</remarks>
    /// <param name="pagina">Número de página. Empieza en 1.</param>
    /// <param name="tamano">Cantidad de clientes por página. Máximo 100, por defecto 20.</param>
    /// <response code="200">Lista paginada de clientes con el total de registros y páginas.</response>
    /// <response code="400">Los parámetros de paginación son inválidos.</response>
    [HttpGet]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> Obtener([FromQuery] int pagina = 1, [FromQuery] int tamano = 20)
    {
        var resultado = await _mediator.Send(new ObtenerClientesQuery(pagina, tamano));
        return Ok(resultado);
    }

    /// <summary>Obtiene el detalle de un cliente.</summary>
    /// <response code="200">Datos del cliente: nombre, teléfono, email y fecha de registro.</response>
    /// <response code="404">No existe ningún cliente con ese id.</response>
    [HttpGet("{id}")]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> ObtenerPorId(Guid id)
    {
        var cliente = await _mediator.Send(new ObtenerClientePorIdQuery(id));
        return Ok(cliente);
    }

    /// <summary>Registra un nuevo cliente en la sastrería.</summary>
    /// <response code="201">Cliente creado correctamente. Devuelve el id asignado.</response>
    /// <response code="400">Los datos son inválidos (nombre vacío, email incorrecto, etc.).</response>
    [HttpPost]
    [ProducesResponseType(StatusCodes.Status201Created)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> Crear([FromBody] CrearClienteCommand command)
    {
        var id = await _mediator.Send(command);
        return StatusCode(StatusCodes.Status201Created, new { id });
    }

    /// <summary>Actualiza los datos de un cliente.</summary>
    /// <response code="204">Cliente actualizado correctamente.</response>
    /// <response code="400">Los datos son inválidos.</response>
    /// <response code="404">No existe ningún cliente con ese id.</response>
    [HttpPut("{id}")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Actualizar(Guid id, [FromBody] ActualizarClienteCommand command)
    {
        await _mediator.Send(command with { Id = id });
        return NoContent();
    }

    /// <summary>Elimina un cliente de la sastrería.</summary>
    /// <remarks>El cliente no se borra físicamente de la base de datos, solo se marca como eliminado y deja de aparecer en las listas.</remarks>
    /// <response code="204">Cliente eliminado correctamente.</response>
    /// <response code="404">No existe ningún cliente con ese id.</response>
    [HttpDelete("{id}")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Eliminar(Guid id)
    {
        await _mediator.Send(new EliminarClienteCommand(id));
        return NoContent();
    }
}
