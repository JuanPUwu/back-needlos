using MediatR;
using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.Excepciones;

namespace Needlos.Aplicacion.MedidasCliente.Comandos.ActualizarMedidasCliente;

public class ActualizarMedidasClienteHandler : IRequestHandler<ActualizarMedidasClienteCommand, Unit>
{
    private readonly INeedlosDbContext _context;

    public ActualizarMedidasClienteHandler(INeedlosDbContext context)
    {
        _context = context;
    }

    public async Task<Unit> Handle(ActualizarMedidasClienteCommand request, CancellationToken cancellationToken)
    {
        var medidas = await _context.MedidasClientes
            .FirstOrDefaultAsync(m => m.Id == request.Id, cancellationToken);

        if (medidas is null)
            throw new NotFoundException($"Medidas '{request.Id}' no encontradas.");

        medidas.Pecho = request.Pecho;
        medidas.Cintura = request.Cintura;
        medidas.Largo = request.Largo;
        medidas.Observaciones = request.Observaciones;

        await _context.SaveChangesAsync(cancellationToken);

        return Unit.Value;
    }
}
