using Needlos.Aplicacion.Admin.DTOs;

namespace Needlos.Aplicacion.Contratos;

public interface IEstadisticasBdService
{
    Task<EstadisticasBdDto> ObtenerEstadisticasAsync(CancellationToken ct = default);
}
