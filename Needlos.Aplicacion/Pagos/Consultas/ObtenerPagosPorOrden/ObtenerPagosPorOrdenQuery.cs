using MediatR;
using Needlos.Aplicacion.Pagos.DTOs;

namespace Needlos.Aplicacion.Pagos.Consultas.ObtenerPagosPorOrden;

public record ObtenerPagosPorOrdenQuery(Guid OrdenId) : IRequest<List<PagoDto>>;
