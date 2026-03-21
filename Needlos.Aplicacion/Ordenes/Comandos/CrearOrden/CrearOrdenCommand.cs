using MediatR;

namespace Needlos.Aplicacion.Ordenes.Comandos.CrearOrden;

public record DetalleOrdenRequest(Guid ServicioId, decimal Precio, string Notas);

public record CrearOrdenCommand(
    Guid ClienteId,
    DateTime FechaEntrega,
    List<DetalleOrdenRequest> Detalles
) : IRequest<Guid>;
