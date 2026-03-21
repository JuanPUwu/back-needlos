using MediatR;
using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.Excepciones;

namespace Needlos.Aplicacion.Clientes.Comandos.ActualizarCliente;

public class ActualizarClienteHandler : IRequestHandler<ActualizarClienteCommand, Unit>
{
    private readonly INeedlosDbContext _context;

    public ActualizarClienteHandler(INeedlosDbContext context)
    {
        _context = context;
    }

    public async Task<Unit> Handle(ActualizarClienteCommand request, CancellationToken cancellationToken)
    {
        var cliente = await _context.Clientes
            .FirstOrDefaultAsync(c => c.Id == request.Id, cancellationToken);

        if (cliente is null)
            throw new NotFoundException($"Cliente '{request.Id}' no encontrado.");

        cliente.Nombre = request.Nombre;
        cliente.Telefono = request.Telefono;
        cliente.Email = request.Email;

        await _context.SaveChangesAsync(cancellationToken);

        return Unit.Value;
    }
}
