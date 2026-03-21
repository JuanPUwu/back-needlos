using MediatR;

namespace Needlos.Aplicacion.Clientes.Comandos.ActualizarCliente;

public record ActualizarClienteCommand(
    Guid Id,
    string Nombre,
    string Telefono,
    string Email
) : IRequest<Unit>;
