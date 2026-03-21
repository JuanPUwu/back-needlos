using MediatR;
using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.Excepciones;

namespace Needlos.Aplicacion.Servicios.Comandos.ActualizarServicio;

public class ActualizarServicioHandler : IRequestHandler<ActualizarServicioCommand, Unit>
{
    private readonly INeedlosDbContext _context;

    public ActualizarServicioHandler(INeedlosDbContext context)
    {
        _context = context;
    }

    public async Task<Unit> Handle(ActualizarServicioCommand request, CancellationToken cancellationToken)
    {
        var servicio = await _context.Servicios
            .FirstOrDefaultAsync(s => s.Id == request.Id, cancellationToken);

        if (servicio is null)
            throw new NotFoundException($"Servicio '{request.Id}' no encontrado.");

        servicio.Nombre = request.Nombre;
        servicio.PrecioBase = request.PrecioBase;

        await _context.SaveChangesAsync(cancellationToken);

        return Unit.Value;
    }
}
