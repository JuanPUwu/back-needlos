using MediatR;
using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.Excepciones;
using Needlos.Aplicacion.Shared;
using Needlos.Dominio.Entidades;

namespace Needlos.Aplicacion.Ordenes.Comandos.CrearOrden;

public class CrearOrdenHandler : IRequestHandler<CrearOrdenCommand, Guid>
{
    private readonly INeedlosDbContext _context;
    private readonly ClienteService   _clienteService;

    public CrearOrdenHandler(INeedlosDbContext context, ClienteService clienteService)
    {
        _context        = context;
        _clienteService = clienteService;
    }

    public async Task<Guid> Handle(CrearOrdenCommand request, CancellationToken cancellationToken)
    {
        await _clienteService.ValidarExistenciaAsync(request.ClienteId, cancellationToken);

        // Validar que todos los TipoPrendaId existen
        var tiposIds = request.Prendas.Select(p => p.TipoPrendaId).Distinct().ToList();
        var tiposExistentes = await _context.TiposPrendas
            .Where(t => tiposIds.Contains(t.Id))
            .Select(t => t.Id)
            .ToListAsync(cancellationToken);

        var tipoFaltante = tiposIds.FirstOrDefault(id => !tiposExistentes.Contains(id));
        if (tipoFaltante != default)
            throw new NotFoundException($"Tipo de prenda '{tipoFaltante}' no encontrado.");

        var orden = new Orden
        {
            Id        = Guid.NewGuid(),
            ClienteId = request.ClienteId,
            TipoOrden = request.TipoOrden
        };

        foreach (var p in request.Prendas)
        {
            orden.Prendas.Add(new Prenda
            {
                Id             = Guid.NewGuid(),
                OrdenId        = orden.Id,
                TipoPrendaId   = p.TipoPrendaId,
                Cantidad       = p.Cantidad,
                Descripcion    = p.Descripcion,
                PrecioPorUnidad = p.PrecioPorUnidad,
                FechaEntrega   = p.FechaEntrega
            });
        }

        _context.Ordenes.Add(orden);
        await _context.SaveChangesAsync(cancellationToken);

        return orden.Id;
    }
}
