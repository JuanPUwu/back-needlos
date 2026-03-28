using MediatR;
using Needlos.Aplicacion.Admin.DTOs;
using Needlos.Aplicacion.Contratos;

namespace Needlos.Aplicacion.Admin.Consultas.ObtenerEstadisticasBd;

public class ObtenerEstadisticasBdHandler(IEstadisticasBdService estadisticasService)
    : IRequestHandler<ObtenerEstadisticasBdQuery, EstadisticasBdDto>
{
    public Task<EstadisticasBdDto> Handle(ObtenerEstadisticasBdQuery request, CancellationToken ct) =>
        estadisticasService.ObtenerEstadisticasAsync(ct);
}
