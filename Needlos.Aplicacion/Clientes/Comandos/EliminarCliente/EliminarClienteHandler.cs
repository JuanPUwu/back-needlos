using MediatR;
using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.Excepciones;

namespace Needlos.Aplicacion.Clientes.Comandos.EliminarCliente;

public class EliminarClienteHandler : IRequestHandler<EliminarClienteCommand>
{
    private readonly INeedlosDbContext _context;

    public EliminarClienteHandler(INeedlosDbContext context)
    {
        _context = context;
    }

    public async Task Handle(EliminarClienteCommand request, CancellationToken cancellationToken)
    {
        var cliente = await _context.Clientes
            .FirstOrDefaultAsync(c => c.Id == request.Id, cancellationToken);

        if (cliente is null)
            throw new NotFoundException($"Cliente '{request.Id}' no encontrado.");

        cliente.Eliminado = true;
        await _context.SaveChangesAsync(cancellationToken);
    }
}
