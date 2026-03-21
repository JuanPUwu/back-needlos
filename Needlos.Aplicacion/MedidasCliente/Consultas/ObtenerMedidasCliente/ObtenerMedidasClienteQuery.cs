using MediatR;
using Needlos.Aplicacion.MedidasCliente.DTOs;

namespace Needlos.Aplicacion.MedidasCliente.Consultas.ObtenerMedidasCliente;

public record ObtenerMedidasClienteQuery(Guid ClienteId) : IRequest<List<MedidasClienteDto>>;
