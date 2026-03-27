using MediatR;
using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.Excepciones;

namespace Needlos.Aplicacion.Ordenes.Comandos.CambiarEstadoPrenda;

public class CambiarEstadoPrendaHandler : IRequestHandler<CambiarEstadoPrendaCommand, Unit>
{
    private readonly INeedlosDbContext _context;

    public CambiarEstadoPrendaHandler(INeedlosDbContext context)
    {
        _context = context;
    }

    public async Task<Unit> Handle(CambiarEstadoPrendaCommand request, CancellationToken cancellationToken)
    {
        // Verificar que la orden existe en el tenant
        var ordenExiste = await _context.Ordenes
            .AnyAsync(o => o.Id == request.OrdenId, cancellationToken);

        if (!ordenExiste)
            throw new NotFoundException($"Orden '{request.OrdenId}' no encontrada.");

        var prenda = await _context.Prendas
            .FirstOrDefaultAsync(p => p.Id == request.PrendaId && p.OrdenId == request.OrdenId, cancellationToken);

        if (prenda is null)
            throw new NotFoundException($"Prenda '{request.PrendaId}' no encontrada en la orden.");

        prenda.CambiarEstado(request.NuevoEstado);

        await _context.SaveChangesAsync(cancellationToken);

        return Unit.Value;
    }
}
