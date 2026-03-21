using MediatR;
using Needlos.Aplicacion.Servicios.DTOs;

namespace Needlos.Aplicacion.Servicios.Consultas.ObtenerServicioPorId;

public record ObtenerServicioPorIdQuery(Guid Id) : IRequest<ServicioDto>;
