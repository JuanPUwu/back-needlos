using MediatR;
using Needlos.Dominio.Enumeraciones;

namespace Needlos.Aplicacion.Ordenes.Comandos.CambiarEstadoPrenda;

public record CambiarEstadoPrendaCommand(
    Guid         OrdenId,
    Guid         PrendaId,
    EstadoPrenda NuevoEstado
) : IRequest<Unit>;
