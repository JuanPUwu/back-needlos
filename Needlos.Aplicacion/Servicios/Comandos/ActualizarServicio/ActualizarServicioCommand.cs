using MediatR;

namespace Needlos.Aplicacion.Servicios.Comandos.ActualizarServicio;

public record ActualizarServicioCommand(
    Guid Id,
    string Nombre,
    decimal PrecioBase
) : IRequest<Unit>;
