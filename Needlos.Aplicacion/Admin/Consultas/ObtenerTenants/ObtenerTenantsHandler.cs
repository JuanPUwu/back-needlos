using MediatR;
using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Admin.DTOs;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.Shared;

namespace Needlos.Aplicacion.Admin.Consultas.ObtenerTenants;

public class ObtenerTenantsHandler : IRequestHandler<ObtenerTenantsQuery, PaginadoDto<TenantAdminDto>>
{
    private readonly INeedlosDbContext _context;

    public ObtenerTenantsHandler(INeedlosDbContext context)
    {
        _context = context;
    }

    public async Task<PaginadoDto<TenantAdminDto>> Handle(ObtenerTenantsQuery request, CancellationToken cancellationToken)
    {
        var query = _context.Tenants.OrderByDescending(t => t.CreadoEn);

        var total = await query.CountAsync(cancellationToken);

        var datos = await query
            .Skip((request.Pagina - 1) * request.Tamano)
            .Take(request.Tamano)
            .Select(t => new TenantAdminDto
            {
                Id       = t.Id,
                Nombre   = t.Nombre,
                Slug     = t.Slug,
                Activo   = t.Activo,
                CreadoEn = t.CreadoEn
            })
            .ToListAsync(cancellationToken);

        return new PaginadoDto<TenantAdminDto>
        {
            Datos  = datos,
            Pagina = request.Pagina,
            Tamano = request.Tamano,
            Total  = total
        };
    }
}
