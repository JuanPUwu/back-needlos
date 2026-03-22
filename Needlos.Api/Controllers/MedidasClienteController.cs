using MediatR;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Needlos.Aplicacion.MedidasCliente.Comandos.ActualizarMedidasCliente;
using Needlos.Aplicacion.MedidasCliente.Comandos.CrearMedidasCliente;
using Needlos.Aplicacion.MedidasCliente.Comandos.EliminarMedidasCliente;
using Needlos.Aplicacion.MedidasCliente.Consultas.ObtenerMedidasCliente;

namespace Needlos.Api.Controllers;

[ApiController]
[Route("api/clientes/{clienteId}/medidas")]
[Authorize(Roles = "Admin,SuperAdmin")]
public class MedidasClienteController : ControllerBase
{
    private readonly IMediator _mediator;

    public MedidasClienteController(IMediator mediator)
    {
        _mediator = mediator;
    }

    /// <summary>Lista todas las medidas registradas para un cliente.</summary>
    /// <remarks>Un cliente puede tener múltiples registros de medidas a lo largo del tiempo.</remarks>
    /// <response code="200">Lista de medidas con pecho, cintura, largo y observaciones.</response>
    /// <response code="404">No existe ningún cliente con ese clienteId.</response>
    [HttpGet]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Obtener(Guid clienteId)
    {
        var medidas = await _mediator.Send(new ObtenerMedidasClienteQuery(clienteId));
        return Ok(medidas);
    }

    /// <summary>Registra un nuevo set de medidas para un cliente.</summary>
    /// <remarks>Todos los valores de pecho, cintura y largo deben ser mayores a cero. Las observaciones son opcionales.</remarks>
    /// <response code="201">Medidas registradas correctamente. Devuelve el id asignado.</response>
    /// <response code="400">Los datos son inválidos (medidas en cero o negativas).</response>
    /// <response code="404">No existe ningún cliente con ese clienteId.</response>
    [HttpPost]
    [ProducesResponseType(StatusCodes.Status201Created)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Crear(Guid clienteId, [FromBody] CrearMedidasClienteCommand command)
    {
        var id = await _mediator.Send(command with { ClienteId = clienteId });
        return StatusCode(StatusCodes.Status201Created, new { id });
    }

    /// <summary>Actualiza un registro de medidas existente.</summary>
    /// <response code="204">Medidas actualizadas correctamente.</response>
    /// <response code="400">Los datos son inválidos.</response>
    /// <response code="404">No existe ningún registro de medidas con ese id.</response>
    [HttpPut("{id}")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Actualizar(Guid clienteId, Guid id, [FromBody] ActualizarMedidasClienteCommand command)
    {
        await _mediator.Send(command with { Id = id });
        return NoContent();
    }

    /// <summary>Elimina un registro de medidas.</summary>
    /// <remarks>El registro no se borra físicamente, solo deja de aparecer en las consultas.</remarks>
    /// <response code="204">Medidas eliminadas correctamente.</response>
    /// <response code="404">No existe ningún registro de medidas con ese id.</response>
    [HttpDelete("{id}")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Eliminar(Guid clienteId, Guid id)
    {
        await _mediator.Send(new EliminarMedidasClienteCommand(id));
        return NoContent();
    }
}
