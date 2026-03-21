using MediatR;
using Needlos.Aplicacion.Contratos;
using Needlos.Dominio.Entidades;

namespace Needlos.Aplicacion.Servicios.Comandos.CrearServicio;

public class CrearServicioHandler : IRequestHandler<CrearServicioCommand, Guid>
{
    private readonly INeedlosDbContext _context;

    public CrearServicioHandler(INeedlosDbContext context)
    {
        _context = context;
    }

    public async Task<Guid> Handle(CrearServicioCommand request, CancellationToken cancellationToken)
    {
        var servicio = new Servicio
        {
            Id = Guid.NewGuid(),
            Nombre = request.Nombre,
            PrecioBase = request.PrecioBase
        };

        _context.Servicios.Add(servicio);
        await _context.SaveChangesAsync(cancellationToken);

        return servicio.Id;
    }
}
