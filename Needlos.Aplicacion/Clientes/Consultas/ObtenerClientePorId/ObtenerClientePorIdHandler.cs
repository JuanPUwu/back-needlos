using MediatR;
using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Clientes.DTOs;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.Excepciones;

namespace Needlos.Aplicacion.Clientes.Consultas.ObtenerClientePorId;

public class ObtenerClientePorIdHandler : IRequestHandler<ObtenerClientePorIdQuery, ClienteDto>
{
    private readonly INeedlosDbContext _context;

    public ObtenerClientePorIdHandler(INeedlosDbContext context)
    {
        _context = context;
    }

    public async Task<ClienteDto> Handle(ObtenerClientePorIdQuery request, CancellationToken cancellationToken)
    {
        var cliente = await _context.Clientes
            .Where(c => c.Id == request.Id)
            .Select(c => new ClienteDto
            {
                Id = c.Id,
                Nombre = c.Nombre,
                Telefono = c.Telefono,
                Email = c.Email,
                FechaRegistro = c.FechaRegistro
            })
            .FirstOrDefaultAsync(cancellationToken);

        if (cliente is null)
            throw new NotFoundException($"Cliente '{request.Id}' no encontrado.");

        return cliente;
    }
}
