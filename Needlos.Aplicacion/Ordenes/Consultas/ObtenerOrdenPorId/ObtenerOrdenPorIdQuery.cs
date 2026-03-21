using MediatR;
using Needlos.Aplicacion.Ordenes.DTOs;

namespace Needlos.Aplicacion.Ordenes.Consultas.ObtenerOrdenPorId;

public record ObtenerOrdenPorIdQuery(Guid Id) : IRequest<OrdenDto>;
