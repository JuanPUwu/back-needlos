using MediatR;
using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.Ordenes.DTOs;
using Needlos.Aplicacion.Shared;
using Needlos.Dominio.Entidades;
using Needlos.Dominio.Enumeraciones;

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
        var total = await _context.Ordenes.CountAsync(cancellationToken);

        var ordenes = await _context.Ordenes
            .Include(o => o.Cliente)
            .Include(o => o.Prendas)
                .ThenInclude(p => p.TipoPrenda)
            .OrderByDescending(o => o.CreadoEn)
            .Skip((request.Pagina - 1) * request.Tamano)
            .Take(request.Tamano)
            .ToListAsync(cancellationToken);

        var datos = ordenes.Select(o => MapearOrden(o)).ToList();

        return new PaginadoDto<OrdenDto>
        {
            Datos   = datos,
            Pagina  = request.Pagina,
            Tamano  = request.Tamano,
            Total   = total
        };
    }

    internal static OrdenDto MapearOrden(Orden o)
    {
        var prendas = o.Prendas.Select(p => new PrendaDto
        {
            Id              = p.Id,
            TipoPrendaId    = p.TipoPrendaId,
            TipoPrenda      = p.TipoPrenda!.Nombre,
            Cantidad        = p.Cantidad,
            Descripcion     = p.Descripcion,
            PrecioPorUnidad = p.PrecioPorUnidad,
            PrecioTotal     = p.PrecioPorUnidad * p.Cantidad,
            FechaEntrega    = p.FechaEntrega,
            Estado          = p.Estado.ToString()
        }).ToList();

        // Estado de la orden = estado más joven (menor valor enum) entre todas las prendas
        var estadoOrden = prendas.Count > 0
            ? o.Prendas.Min(p => p.Estado)
            : EstadoPrenda.EnProceso;

        // Fecha de entrega = la más próxima entre todas las prendas
        var fechaEntrega = prendas.Count > 0
            ? o.Prendas.Min(p => p.FechaEntrega)
            : DateOnly.FromDateTime(DateTime.UtcNow);

        var precioTotal = prendas.Sum(p => p.PrecioTotal);

        return new OrdenDto
        {
            Id              = o.Id,
            ClienteId       = o.ClienteId,
            NombreCliente   = o.Cliente!.Nombre,
            ApellidoCliente = o.Cliente.Apellido,
            TipoOrden       = o.TipoOrden.ToString(),
            Estado          = estadoOrden.ToString(),
            PrecioTotal     = precioTotal,
            FechaEntrega    = fechaEntrega,
            CreadoEn        = o.CreadoEn,
            Prendas         = prendas
        };
    }
}
