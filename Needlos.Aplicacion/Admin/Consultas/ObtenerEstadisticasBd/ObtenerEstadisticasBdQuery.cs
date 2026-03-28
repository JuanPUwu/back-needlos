using MediatR;
using Needlos.Aplicacion.Admin.DTOs;

namespace Needlos.Aplicacion.Admin.Consultas.ObtenerEstadisticasBd;

public record ObtenerEstadisticasBdQuery : IRequest<EstadisticasBdDto>;
