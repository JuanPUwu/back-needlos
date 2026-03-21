using MediatR;
using Needlos.Aplicacion.Clientes.DTOs;

namespace Needlos.Aplicacion.Clientes.Consultas.ObtenerClientePorId;

public record ObtenerClientePorIdQuery(Guid Id) : IRequest<ClienteDto>;
