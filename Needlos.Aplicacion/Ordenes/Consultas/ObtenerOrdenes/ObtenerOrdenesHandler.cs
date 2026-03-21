using MediatR;
using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.Ordenes.DTOs;
using Needlos.Aplicacion.Shared;

namespace Needlos.Aplicacion.Ordenes.Consultas.ObtenerOrdenes;

public class ObtenerOrdenesHandler : IRequestHandler<ObtenerOrdenesQuery, PaginadoDto<OrdenDto>>
{
    private readonly INeedlosDbContext _context;

    public ObtenerOrdenesHandler(INeedlosDbContext context)
    {
        _context = context;
    }

    public async Task<PaginadoDto<OrdenDto>> Handle(ObtenerOrdenesQuery request, CancellationToken cancellationToken)
    {
        // Contamos primero sin materializar los datos (query eficiente)
        var total = await _context.Ordenes.CountAsync(cancellationToken);

        var datos = await _context.Ordenes
            .Include(o => o.Cliente)
            .Include(o => o.Detalles)
                .ThenInclude(d => d.Servicio)
            .OrderByDescending(o => o.CreadoEn)
            .Skip((request.Pagina - 1) * request.Tamano)
            .Take(request.Tamano)
            .Select(o => new OrdenDto
            {
                Id = o.Id,
                ClienteId = o.ClienteId,
                NombreCliente = o.Cliente!.Nombre,
                Estado = o.Estado.ToString(),
                PrecioTotal = o.PrecioTotal,
                FechaEntrega = o.FechaEntrega,
                CreadoEn = o.CreadoEn,
                Detalles = o.Detalles.Select(d => new DetalleOrdenDto
                {
                    Id = d.Id,
                    ServicioId = d.ServicioId,
                    NombreServicio = d.Servicio!.Nombre,
                    Precio = d.Precio,
                    Notas = d.Notas
                }).ToList()
            })
            .ToListAsync(cancellationToken);

        return new PaginadoDto<OrdenDto>
        {
            Datos = datos,
            Pagina = request.Pagina,
            Tamano = request.Tamano,
            Total = total
        };
    }
}
