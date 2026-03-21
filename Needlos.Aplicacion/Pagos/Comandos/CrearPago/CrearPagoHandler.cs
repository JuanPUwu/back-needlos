using MediatR;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.Shared;
using Needlos.Dominio.Entidades;

namespace Needlos.Aplicacion.Pagos.Comandos.CrearPago;

public class CrearPagoHandler : IRequestHandler<CrearPagoCommand, Guid>
{
    private readonly INeedlosDbContext _context;
    private readonly OrdenService _ordenService;

    public CrearPagoHandler(INeedlosDbContext context, OrdenService ordenService)
    {
        _context = context;
        _ordenService = ordenService;
    }

    public async Task<Guid> Handle(CrearPagoCommand request, CancellationToken cancellationToken)
    {
        // La verificación de existencia está centralizada en OrdenService.
        // Si la orden no existe, lanza NotFoundException → 404.
        await _ordenService.ValidarExistenciaAsync(request.OrdenId, cancellationToken);

        var pago = new Pago
        {
            Id = Guid.NewGuid(),
            OrdenId = request.OrdenId,
            Monto = request.Monto,
            Metodo = request.Metodo,
            Fecha = DateTime.UtcNow
        };

        _context.Pagos.Add(pago);
        await _context.SaveChangesAsync(cancellationToken);

        return pago.Id;
    }
}
