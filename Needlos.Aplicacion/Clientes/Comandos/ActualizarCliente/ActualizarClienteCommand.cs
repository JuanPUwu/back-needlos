using MediatR;

namespace Needlos.Aplicacion.Clientes.Comandos.ActualizarCliente;

public record ActualizarClienteCommand(
    Guid   Id,
    string Nombre,
    string Apellido,
    string Telefono
) : IRequest;
