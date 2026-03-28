using MediatR;
using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Contratos;

namespace Needlos.Aplicacion.Admin.Comandos.LimpiarTokensExpirados;

public class LimpiarTokensExpiradosHandler(INeedlosDbContext context)
    : IRequestHandler<LimpiarTokensExpiradosCommand, int>
{
    public Task<int> Handle(LimpiarTokensExpiradosCommand request, CancellationToken ct) =>
        context.RefreshTokens
            .Where(rt => rt.Expira < DateTime.UtcNow)
            .ExecuteDeleteAsync(ct);
}
