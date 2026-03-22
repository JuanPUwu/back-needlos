using MediatR;
using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Admin.DTOs;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.Excepciones;
using Needlos.Aplicacion.Shared;

namespace Needlos.Aplicacion.Admin.Consultas.ObtenerUsuariosPorTenant;

public class ObtenerUsuariosPorTenantHandler
    : IRequestHandler<ObtenerUsuariosPorTenantQuery, PaginadoDto<UsuarioAdminDto>>
{
    private readonly INeedlosDbContext _context;

    public ObtenerUsuariosPorTenantHandler(INeedlosDbContext context)
    {
        _context = context;
    }

    public async Task<PaginadoDto<UsuarioAdminDto>> Handle(
        ObtenerUsuariosPorTenantQuery request, CancellationToken cancellationToken)
    {
        var existeTenant = await _context.Tenants
            .AnyAsync(t => t.Id == request.TenantId, cancellationToken);

        if (!existeTenant)
            throw new NotFoundException($"Tenant '{request.TenantId}' no encontrado.");

        var query = _context.Usuarios
            .Include(u => u.Roles).ThenInclude(ur => ur.Rol)
            .Where(u => u.TenantId == request.TenantId)
            .OrderBy(u => u.Email);

        var total = await query.CountAsync(cancellationToken);

        var datos = await query
            .Skip((request.Pagina - 1) * request.Tamano)
            .Take(request.Tamano)
            .Select(u => new UsuarioAdminDto
            {
                Id     = u.Id,
                Email  = u.Email,
                Activo = u.Activo,
                Roles  = u.Roles.Select(ur => ur.Rol!.Nombre).ToList()
            })
            .ToListAsync(cancellationToken);

        return new PaginadoDto<UsuarioAdminDto>
        {
            Datos  = datos,
            Pagina = request.Pagina,
            Tamano = request.Tamano,
            Total  = total
        };
    }
}
