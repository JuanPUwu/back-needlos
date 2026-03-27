using MediatR;
using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.Excepciones;
using Needlos.Aplicacion.Ordenes.DTOs;
using Needlos.Aplicacion.Ordenes.Consultas.ObtenerOrdenes;

namespace Needlos.Aplicacion.Ordenes.Consultas.ObtenerOrdenPorId;

public class ObtenerOrdenPorIdHandler : IRequestHandler<ObtenerOrdenPorIdQuery, OrdenDto>
{
    private readonly INeedlosDbContext _context;

    public ObtenerOrdenPorIdHandler(INeedlosDbContext context)
    {
        _context = context;
    }

    public async Task<OrdenDto> Handle(ObtenerOrdenPorIdQuery request, CancellationToken cancellationToken)
    {
        var orden = await _context.Ordenes
            .Include(o => o.Cliente)
            .Include(o => o.Prendas)
                .ThenInclude(p => p.TipoPrenda)
            .FirstOrDefaultAsync(o => o.Id == request.Id, cancellationToken);

        if (orden is null)
            throw new NotFoundException($"Orden '{request.Id}' no encontrada.");

        return ObtenerOrdenesHandler.MapearOrden(orden);
    }
}
