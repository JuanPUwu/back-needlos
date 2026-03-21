using MediatR;
using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.Excepciones;
using Needlos.Aplicacion.Ordenes.DTOs;

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
            .Include(o => o.Detalles)
                .ThenInclude(d => d.Servicio)
            .Where(o => o.Id == request.Id)
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
            .FirstOrDefaultAsync(cancellationToken);

        if (orden is null)
            throw new NotFoundException($"Orden '{request.Id}' no encontrada.");

        return orden;
    }
}
