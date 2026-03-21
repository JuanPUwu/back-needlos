using MediatR;
using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.Excepciones;

namespace Needlos.Aplicacion.MedidasCliente.Comandos.EliminarMedidasCliente;

public class EliminarMedidasClienteHandler : IRequestHandler<EliminarMedidasClienteCommand, Unit>
{
    private readonly INeedlosDbContext _context;

    public EliminarMedidasClienteHandler(INeedlosDbContext context)
    {
        _context = context;
    }

    public async Task<Unit> Handle(EliminarMedidasClienteCommand request, CancellationToken cancellationToken)
    {
        var medidas = await _context.MedidasClientes
            .FirstOrDefaultAsync(m => m.Id == request.Id, cancellationToken);

        if (medidas is null)
            throw new NotFoundException($"Medidas '{request.Id}' no encontradas.");

        medidas.Eliminado = true;
        await _context.SaveChangesAsync(cancellationToken);

        return Unit.Value;
    }
}
