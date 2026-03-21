using MediatR;
using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.Excepciones;
using Needlos.Dominio.Entidades;

namespace Needlos.Aplicacion.Ordenes.Comandos.ActualizarEstadoOrden;

public class ActualizarEstadoOrdenHandler : IRequestHandler<ActualizarEstadoOrdenCommand, Unit>
{
    private readonly INeedlosDbContext _context;
    private readonly ITenantProvider _tenantProvider;

    public ActualizarEstadoOrdenHandler(INeedlosDbContext context, ITenantProvider tenantProvider)
    {
        _context = context;
        _tenantProvider = tenantProvider;
    }

    public async Task<Unit> Handle(ActualizarEstadoOrdenCommand request, CancellationToken cancellationToken)
    {
        var orden = await _context.Ordenes
            .FirstOrDefaultAsync(o => o.Id == request.OrdenId, cancellationToken);

        if (orden is null)
            throw new NotFoundException($"Orden '{request.OrdenId}' no encontrada.");

        var estadoAnterior = orden.Estado;

        // La validación de la transición la hace la propia entidad (dominio rico).
        // Si el estado es inválido, Orden.CambiarEstado lanza BusinessException → 400.
        orden.CambiarEstado(request.NuevoEstado);

        _context.HistorialesEstadoOrden.Add(new HistorialEstadoOrden
        {
            Id = Guid.NewGuid(),
            OrdenId = orden.Id,
            EstadoAnterior = estadoAnterior,
            EstadoNuevo = request.NuevoEstado
        });

        await _context.SaveChangesAsync(cancellationToken);

        return Unit.Value;
    }
}
