using MediatR;

namespace Needlos.Aplicacion.Clientes.Comandos.EliminarCliente;

public record EliminarClienteCommand(Guid Id) : IRequest<Unit>;
