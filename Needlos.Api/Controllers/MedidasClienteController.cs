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
[Authorize]
public class MedidasClienteController : ControllerBase
{
    private readonly IMediator _mediator;

    public MedidasClienteController(IMediator mediator)
    {
        _mediator = mediator;
    }

    /// <summary>Devuelve todas las medidas de un cliente.</summary>
    /// <response code="200">Lista de medidas del cliente.</response>
    /// <response code="404">Cliente no encontrado.</response>
    [HttpGet]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Obtener(Guid clienteId)
    {
        var medidas = await _mediator.Send(new ObtenerMedidasClienteQuery(clienteId));
        return Ok(medidas);
    }

    /// <summary>Registra nuevas medidas para un cliente.</summary>
    /// <response code="201">Medidas creadas. Devuelve el id.</response>
    /// <response code="400">Datos inválidos.</response>
    /// <response code="404">Cliente no encontrado.</response>
    [HttpPost]
    [ProducesResponseType(StatusCodes.Status201Created)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Crear(Guid clienteId, [FromBody] CrearMedidasClienteCommand command)
    {
        var id = await _mediator.Send(command with { ClienteId = clienteId });
        return StatusCode(StatusCodes.Status201Created, new { id });
    }

    /// <summary>Actualiza unas medidas existentes.</summary>
    /// <response code="204">Medidas actualizadas.</response>
    /// <response code="400">Datos inválidos.</response>
    /// <response code="404">Medidas no encontradas.</response>
    [HttpPut("{id}")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Actualizar(Guid clienteId, Guid id, [FromBody] ActualizarMedidasClienteCommand command)
    {
        await _mediator.Send(command with { Id = id });
        return NoContent();
    }

    /// <summary>Elimina (soft delete) unas medidas.</summary>
    /// <response code="204">Medidas eliminadas.</response>
    /// <response code="404">Medidas no encontradas.</response>
    [HttpDelete("{id}")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Eliminar(Guid clienteId, Guid id)
    {
        await _mediator.Send(new EliminarMedidasClienteCommand(id));
        return NoContent();
    }
}
