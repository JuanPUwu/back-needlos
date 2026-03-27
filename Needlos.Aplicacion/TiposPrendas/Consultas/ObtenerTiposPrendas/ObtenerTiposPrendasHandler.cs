using MediatR;
using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.TiposPrendas.DTOs;

namespace Needlos.Aplicacion.TiposPrendas.Consultas.ObtenerTiposPrendas;

public class ObtenerTiposPrendasHandler : IRequestHandler<ObtenerTiposPrendasQuery, List<TipoPrendaDto>>
{
    private readonly INeedlosDbContext _context;

    public ObtenerTiposPrendasHandler(INeedlosDbContext context)
    {
        _context = context;
    }

    public async Task<List<TipoPrendaDto>> Handle(ObtenerTiposPrendasQuery request, CancellationToken cancellationToken)
    {
        return await _context.TiposPrendas
            .OrderBy(t => t.Nombre)
            .Select(t => new TipoPrendaDto { Id = t.Id, Nombre = t.Nombre })
            .ToListAsync(cancellationToken);
    }
}
