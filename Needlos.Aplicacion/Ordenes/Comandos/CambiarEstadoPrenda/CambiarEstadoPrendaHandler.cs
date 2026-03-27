using MediatR;
using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.Excepciones;
using Needlos.Aplicacion.Shared;

namespace Needlos.Aplicacion.Ordenes.Comandos.CambiarEstadoPrenda;

public class CambiarEstadoPrendaHandler : IRequestHandler<CambiarEstadoPrendaCommand>
{
    private readonly INeedlosDbContext _context;
    private readonly OrdenService _ordenService;

    public CambiarEstadoPrendaHandler(INeedlosDbContext context, OrdenService ordenService)
    {
        _context = context;
        _ordenService = ordenService;
    }

    public async Task Handle(CambiarEstadoPrendaCommand request, CancellationToken cancellationToken)
    {
        await _ordenService.ValidarExistenciaAsync(request.OrdenId, cancellationToken);

        var prenda = await _context.Prendas
            .FirstOrDefaultAsync(p => p.Id == request.PrendaId && p.OrdenId == request.OrdenId, cancellationToken);

        if (prenda is null)
            throw new NotFoundException($"Prenda '{request.PrendaId}' no encontrada en la orden.");

        prenda.CambiarEstado(request.NuevoEstado);

        await _context.SaveChangesAsync(cancellationToken);
    }
}
