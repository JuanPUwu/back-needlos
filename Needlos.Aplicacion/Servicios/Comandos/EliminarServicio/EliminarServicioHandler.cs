using MediatR;
using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.Excepciones;

namespace Needlos.Aplicacion.Servicios.Comandos.EliminarServicio;

public class EliminarServicioHandler : IRequestHandler<EliminarServicioCommand, Unit>
{
    private readonly INeedlosDbContext _context;

    public EliminarServicioHandler(INeedlosDbContext context)
    {
        _context = context;
    }

    public async Task<Unit> Handle(EliminarServicioCommand request, CancellationToken cancellationToken)
    {
        var servicio = await _context.Servicios
            .FirstOrDefaultAsync(s => s.Id == request.Id, cancellationToken);

        if (servicio is null)
            throw new NotFoundException($"Servicio '{request.Id}' no encontrado.");

        servicio.Eliminado = true;
        await _context.SaveChangesAsync(cancellationToken);

        return Unit.Value;
    }
}
