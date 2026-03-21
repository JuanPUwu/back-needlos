using MediatR;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.Shared;
using Needlos.Dominio.Entidades;

namespace Needlos.Aplicacion.Ordenes.Comandos.CrearOrden;

public class CrearOrdenHandler : IRequestHandler<CrearOrdenCommand, Guid>
{
    private readonly INeedlosDbContext _context;
    private readonly ClienteService _clienteService;
    private readonly ServicioService _servicioService;

    public CrearOrdenHandler(INeedlosDbContext context, ClienteService clienteService, ServicioService servicioService)
    {
        _context = context;
        _clienteService = clienteService;
        _servicioService = servicioService;
    }

    public async Task<Guid> Handle(CrearOrdenCommand request, CancellationToken cancellationToken)
    {
        await _clienteService.ValidarExistenciaAsync(request.ClienteId, cancellationToken);

        foreach (var detalle in request.Detalles)
            await _servicioService.ValidarExistenciaAsync(detalle.ServicioId, cancellationToken);

        var orden = new Orden
        {
            Id = Guid.NewGuid(),
            ClienteId = request.ClienteId,
            FechaEntrega = request.FechaEntrega,
            PrecioTotal = request.Detalles.Sum(d => d.Precio)
        };

        foreach (var detalle in request.Detalles)
        {
            orden.Detalles.Add(new DetalleOrden
            {
                Id = Guid.NewGuid(),
                OrdenId = orden.Id,
                ServicioId = detalle.ServicioId,
                Precio = detalle.Precio,
                Notas = detalle.Notas
            });
        }

        _context.Ordenes.Add(orden);
        await _context.SaveChangesAsync(cancellationToken);

        return orden.Id;
    }
}
