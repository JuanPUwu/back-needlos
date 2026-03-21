using MediatR;
using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.MedidasCliente.DTOs;
using Needlos.Aplicacion.Shared;

namespace Needlos.Aplicacion.MedidasCliente.Consultas.ObtenerMedidasCliente;

public class ObtenerMedidasClienteHandler : IRequestHandler<ObtenerMedidasClienteQuery, List<MedidasClienteDto>>
{
    private readonly INeedlosDbContext _context;
    private readonly ClienteService _clienteService;

    public ObtenerMedidasClienteHandler(INeedlosDbContext context, ClienteService clienteService)
    {
        _context = context;
        _clienteService = clienteService;
    }

    public async Task<List<MedidasClienteDto>> Handle(ObtenerMedidasClienteQuery request, CancellationToken cancellationToken)
    {
        await _clienteService.ValidarExistenciaAsync(request.ClienteId, cancellationToken);

        return await _context.MedidasClientes
            .Where(m => m.ClienteId == request.ClienteId)
            .Select(m => new MedidasClienteDto
            {
                Id = m.Id,
                ClienteId = m.ClienteId,
                Pecho = m.Pecho,
                Cintura = m.Cintura,
                Largo = m.Largo,
                Observaciones = m.Observaciones
            })
            .ToListAsync(cancellationToken);
    }
}
