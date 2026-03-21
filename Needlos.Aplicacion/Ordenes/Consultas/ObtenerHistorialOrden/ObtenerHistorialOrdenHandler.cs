using MediatR;
using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.Excepciones;
using Needlos.Aplicacion.Ordenes.DTOs;

namespace Needlos.Aplicacion.Ordenes.Consultas.ObtenerHistorialOrden;

public class ObtenerHistorialOrdenHandler : IRequestHandler<ObtenerHistorialOrdenQuery, List<HistorialEstadoOrdenDto>>
{
    private readonly INeedlosDbContext _context;

    public ObtenerHistorialOrdenHandler(INeedlosDbContext context)
    {
        _context = context;
    }

    public async Task<List<HistorialEstadoOrdenDto>> Handle(ObtenerHistorialOrdenQuery request, CancellationToken cancellationToken)
    {
        var existeOrden = await _context.Ordenes
            .AnyAsync(o => o.Id == request.OrdenId, cancellationToken);

        if (!existeOrden)
            throw new NotFoundException($"Orden '{request.OrdenId}' no encontrada.");

        return await _context.HistorialesEstadoOrden
            .Where(h => h.OrdenId == request.OrdenId)
            .OrderBy(h => h.CreadoEn)
            .Select(h => new HistorialEstadoOrdenDto
            {
                Id = h.Id,
                EstadoAnterior = h.EstadoAnterior.ToString(),
                EstadoNuevo = h.EstadoNuevo.ToString(),
                CambiadoPor = h.CreadoPor,
                CambiadoEn = h.CreadoEn
            })
            .ToListAsync(cancellationToken);
    }
}
