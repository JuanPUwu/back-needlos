using MediatR;
using Needlos.Dominio.Enumeraciones;

namespace Needlos.Aplicacion.Ordenes.Comandos.CrearOrden;

public record PrendaRequest(
    Guid     TipoPrendaId,
    int      Cantidad,
    string   Descripcion,
    decimal  PrecioPorUnidad,
    DateOnly FechaEntrega
);

public record CrearOrdenCommand(
    Guid            ClienteId,
    TipoOrden       TipoOrden,
    List<PrendaRequest> Prendas
) : IRequest<Guid>;
