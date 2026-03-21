using MediatR;
using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.Servicios.DTOs;
using Needlos.Aplicacion.Shared;

namespace Needlos.Aplicacion.Servicios.Consultas.ObtenerServicios;

public class ObtenerServiciosHandler : IRequestHandler<ObtenerServiciosQuery, PaginadoDto<ServicioDto>>
{
    private readonly INeedlosDbContext _context;

    public ObtenerServiciosHandler(INeedlosDbContext context)
    {
        _context = context;
    }

    public async Task<PaginadoDto<ServicioDto>> Handle(ObtenerServiciosQuery request, CancellationToken cancellationToken)
    {
        var total = await _context.Servicios.CountAsync(cancellationToken);

        var datos = await _context.Servicios
            .OrderBy(s => s.Nombre)
            .Skip((request.Pagina - 1) * request.Tamano)
            .Take(request.Tamano)
            .Select(s => new ServicioDto
            {
                Id = s.Id,
                Nombre = s.Nombre,
                PrecioBase = s.PrecioBase
            })
            .ToListAsync(cancellationToken);

        return new PaginadoDto<ServicioDto>
        {
            Datos = datos,
            Pagina = request.Pagina,
            Tamano = request.Tamano,
            Total = total
        };
    }
}
