using MediatR;

namespace Needlos.Aplicacion.Servicios.Comandos.EliminarServicio;

public record EliminarServicioCommand(Guid Id) : IRequest<Unit>;
