using MediatR;

namespace Needlos.Aplicacion.MedidasCliente.Comandos.CrearMedidasCliente;

public record CrearMedidasClienteCommand(
    Guid ClienteId,
    decimal Pecho,
    decimal Cintura,
    decimal Largo,
    string Observaciones
) : IRequest<Guid>;
