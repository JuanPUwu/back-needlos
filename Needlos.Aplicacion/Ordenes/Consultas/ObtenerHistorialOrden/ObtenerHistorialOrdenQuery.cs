using MediatR;
using Needlos.Aplicacion.Ordenes.DTOs;

namespace Needlos.Aplicacion.Ordenes.Consultas.ObtenerHistorialOrden;

public record ObtenerHistorialOrdenQuery(Guid OrdenId) : IRequest<List<HistorialEstadoOrdenDto>>;
