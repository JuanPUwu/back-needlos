using MediatR;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.Shared;

namespace Needlos.Aplicacion.MedidasCliente.Comandos.CrearMedidasCliente;

public class CrearMedidasClienteHandler : IRequestHandler<CrearMedidasClienteCommand, Guid>
{
    private readonly INeedlosDbContext _context;
    private readonly ClienteService _clienteService;

    public CrearMedidasClienteHandler(INeedlosDbContext context, ClienteService clienteService)
    {
        _context = context;
        _clienteService = clienteService;
    }

    public async Task<Guid> Handle(CrearMedidasClienteCommand request, CancellationToken cancellationToken)
    {
        await _clienteService.ValidarExistenciaAsync(request.ClienteId, cancellationToken);

        var medidas = new Dominio.Entidades.MedidasCliente
        {
            Id = Guid.NewGuid(),
            ClienteId = request.ClienteId,
            Pecho = request.Pecho,
            Cintura = request.Cintura,
            Largo = request.Largo,
            Observaciones = request.Observaciones
        };

        _context.MedidasClientes.Add(medidas);
        await _context.SaveChangesAsync(cancellationToken);

        return medidas.Id;
    }
}
