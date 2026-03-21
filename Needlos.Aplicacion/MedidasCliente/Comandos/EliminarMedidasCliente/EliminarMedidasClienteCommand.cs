using MediatR;

namespace Needlos.Aplicacion.MedidasCliente.Comandos.EliminarMedidasCliente;

public record EliminarMedidasClienteCommand(Guid Id) : IRequest<Unit>;
