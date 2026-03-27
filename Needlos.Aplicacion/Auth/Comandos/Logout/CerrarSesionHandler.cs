using MediatR;
using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.Shared;

namespace Needlos.Aplicacion.Auth.Comandos.Logout;

public class CerrarSesionHandler : IRequestHandler<CerrarSesionCommand>
{
    private readonly INeedlosDbContext _context;

    public CerrarSesionHandler(INeedlosDbContext context)
    {
        _context = context;
    }

    public async Task Handle(CerrarSesionCommand request, CancellationToken cancellationToken)
    {
        var tokenHash = TokenHasher.Hash(request.Token);

        var existente = await _context.RefreshTokens
            .FirstOrDefaultAsync(rt => rt.TokenHash == tokenHash, cancellationToken);

        // Idempotente: si ya está usado o no existe, no hacer nada
        if (existente is { Usado: false })
        {
            existente.Usado = true;
            await _context.SaveChangesAsync(cancellationToken);
        }
    }
}
