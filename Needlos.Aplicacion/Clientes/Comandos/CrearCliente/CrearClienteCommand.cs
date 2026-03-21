using MediatR;

namespace Needlos.Aplicacion.Clientes.Comandos.CrearCliente;

public record CrearClienteCommand(
    string Nombre,
    string Telefono,
    string Email
) : IRequest<Guid>;
