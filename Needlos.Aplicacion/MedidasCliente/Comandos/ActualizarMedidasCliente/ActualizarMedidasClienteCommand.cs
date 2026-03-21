using MediatR;

namespace Needlos.Aplicacion.MedidasCliente.Comandos.ActualizarMedidasCliente;

public record ActualizarMedidasClienteCommand(
    Guid Id,
    decimal Pecho,
    decimal Cintura,
    decimal Largo,
    string Observaciones
) : IRequest<Unit>;
