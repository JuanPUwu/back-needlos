using MediatR;
using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.Excepciones;
using Needlos.Aplicacion.Servicios.DTOs;

namespace Needlos.Aplicacion.Servicios.Consultas.ObtenerServicioPorId;

public class ObtenerServicioPorIdHandler : IRequestHandler<ObtenerServicioPorIdQuery, ServicioDto>
{
    private readonly INeedlosDbContext _context;

    public ObtenerServicioPorIdHandler(INeedlosDbContext context)
    {
        _context = context;
    }

    public async Task<ServicioDto> Handle(ObtenerServicioPorIdQuery request, CancellationToken cancellationToken)
    {
        var servicio = await _context.Servicios
            .Where(s => s.Id == request.Id)
            .Select(s => new ServicioDto
            {
                Id = s.Id,
                Nombre = s.Nombre,
                PrecioBase = s.PrecioBase
            })
            .FirstOrDefaultAsync(cancellationToken);

        if (servicio is null)
            throw new NotFoundException($"Servicio '{request.Id}' no encontrado.");

        return servicio;
    }
}
