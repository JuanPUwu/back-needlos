using MediatR;
using Needlos.Aplicacion.Contratos;
using Needlos.Dominio.Entidades;

namespace Needlos.Aplicacion.Clientes.Comandos.CrearCliente;

public class CrearClienteHandler : IRequestHandler<CrearClienteCommand, Guid>
{
    private readonly INeedlosDbContext _context;

    public CrearClienteHandler(INeedlosDbContext context)
    {
        _context = context;
    }

    public async Task<Guid> Handle(CrearClienteCommand request, CancellationToken cancellationToken)
    {
        var cliente = new Cliente
        {
            Id            = Guid.NewGuid(),
            Nombre        = request.Nombre,
            Apellido      = request.Apellido,
            Telefono      = request.Telefono,
            FechaRegistro = DateTime.UtcNow
        };

        _context.Clientes.Add(cliente);
        await _context.SaveChangesAsync(cancellationToken);

        return cliente.Id;
    }
}
