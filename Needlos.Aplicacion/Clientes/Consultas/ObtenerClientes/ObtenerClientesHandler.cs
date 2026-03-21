using MediatR;
using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Clientes.DTOs;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.Shared;

namespace Needlos.Aplicacion.Clientes.Consultas.ObtenerClientes;

public class ObtenerClientesHandler : IRequestHandler<ObtenerClientesQuery, PaginadoDto<ClienteDto>>
{
    private readonly INeedlosDbContext _context;

    public ObtenerClientesHandler(INeedlosDbContext context)
    {
        _context = context;
    }

    public async Task<PaginadoDto<ClienteDto>> Handle(ObtenerClientesQuery request, CancellationToken cancellationToken)
    {
        var total = await _context.Clientes.CountAsync(cancellationToken);

        var datos = await _context.Clientes
            .OrderBy(c => c.Nombre)
            .Skip((request.Pagina - 1) * request.Tamano)
            .Take(request.Tamano)
            .Select(c => new ClienteDto
            {
                Id = c.Id,
                Nombre = c.Nombre,
                Telefono = c.Telefono,
                Email = c.Email,
                FechaRegistro = c.FechaRegistro
            })
            .ToListAsync(cancellationToken);

        return new PaginadoDto<ClienteDto>
        {
            Datos = datos,
            Pagina = request.Pagina,
            Tamano = request.Tamano,
            Total = total
        };
    }
}
