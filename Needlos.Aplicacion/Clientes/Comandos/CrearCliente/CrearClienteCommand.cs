using MediatR;

namespace Needlos.Aplicacion.Clientes.Comandos.CrearCliente;

public record CrearClienteCommand(
    string Nombre,
    string Apellido,
    string Telefono
) : IRequest<Guid>;
