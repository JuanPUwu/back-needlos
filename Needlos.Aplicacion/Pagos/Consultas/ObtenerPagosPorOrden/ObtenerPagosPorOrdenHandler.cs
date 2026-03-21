using MediatR;
using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.Pagos.DTOs;
using Needlos.Aplicacion.Shared;

namespace Needlos.Aplicacion.Pagos.Consultas.ObtenerPagosPorOrden;

public class ObtenerPagosPorOrdenHandler : IRequestHandler<ObtenerPagosPorOrdenQuery, List<PagoDto>>
{
    private readonly INeedlosDbContext _context;
    private readonly OrdenService _ordenService;

    public ObtenerPagosPorOrdenHandler(INeedlosDbContext context, OrdenService ordenService)
    {
        _context = context;
        _ordenService = ordenService;
    }

    public async Task<List<PagoDto>> Handle(ObtenerPagosPorOrdenQuery request, CancellationToken cancellationToken)
    {
        await _ordenService.ValidarExistenciaAsync(request.OrdenId, cancellationToken);

        return await _context.Pagos
            .Where(p => p.OrdenId == request.OrdenId)
            .OrderBy(p => p.Fecha)
            .Select(p => new PagoDto
            {
                Id = p.Id,
                OrdenId = p.OrdenId,
                Monto = p.Monto,
                Metodo = p.Metodo.ToString(),
                Fecha = p.Fecha
            })
            .ToListAsync(cancellationToken);
    }
}
