using MediatR;

namespace Needlos.Aplicacion.Servicios.Comandos.CrearServicio;

public record CrearServicioCommand(string Nombre, decimal PrecioBase) : IRequest<Guid>;
