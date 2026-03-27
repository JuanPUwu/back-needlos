using MediatR;
using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Auth.DTOs;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.Shared;
using Needlos.Dominio.Entidades;

namespace Needlos.Aplicacion.Auth.Comandos.Refresh;

public class RefrescarTokenHandler : IRequestHandler<RefrescarTokenCommand, RefrescarTokenResultDto>
{
    private readonly INeedlosDbContext _context;
    private readonly IJwtService _jwtService;

    public RefrescarTokenHandler(INeedlosDbContext context, IJwtService jwtService)
    {
        _context = context;
        _jwtService = jwtService;
    }

    public async Task<RefrescarTokenResultDto> Handle(RefrescarTokenCommand request, CancellationToken cancellationToken)
    {
        var tokenHash = TokenHasher.Hash(request.Token);

        var existente = await _context.RefreshTokens
            .Include(rt => rt.Usuario)
                .ThenInclude(u => u.Roles)
                    .ThenInclude(ur => ur.Rol)
            .FirstOrDefaultAsync(rt => rt.TokenHash == tokenHash, cancellationToken);

        if (existente is null || existente.Usado || existente.Expira <= DateTime.UtcNow)
            throw new UnauthorizedAccessException("El refresh token es inválido o ha expirado.");

        // Rotar: invalidar el actual y emitir uno nuevo
        existente.Usado = true;

        var nuevoTokenRaw  = TokenHasher.GenerarRaw();
        var nuevoTokenHash = TokenHasher.Hash(nuevoTokenRaw);

        _context.RefreshTokens.Add(new RefreshToken
        {
            Id        = Guid.NewGuid(),
            UsuarioId = existente.UsuarioId,
            TokenHash = nuevoTokenHash,
            Expira    = DateTime.UtcNow.AddDays(7),
            CreadoEn  = DateTime.UtcNow
        });

        await _context.SaveChangesAsync(cancellationToken);

        var rol = existente.Usuario.Roles.FirstOrDefault()?.Rol?.Nombre ?? RolesConstantes.Admin;
        var accessToken = _jwtService.GenerarToken(existente.Usuario, rol);

        return new RefrescarTokenResultDto
        {
            AccessToken     = accessToken,
            RefreshTokenRaw = nuevoTokenRaw
        };
    }
}
