using MediatR;
using Needlos.Aplicacion.Ordenes.DTOs;
using Needlos.Aplicacion.Shared;

namespace Needlos.Aplicacion.Ordenes.Consultas.ObtenerOrdenes;

public record ObtenerOrdenesQuery(int Pagina = 1, int Tamano = 20)
    : IRequest<PaginadoDto<OrdenDto>>, IPaginadoQuery;
