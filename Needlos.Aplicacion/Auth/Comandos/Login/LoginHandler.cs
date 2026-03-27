using MediatR;
using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Auth.DTOs;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.Shared;
using Needlos.Dominio.Entidades;

namespace Needlos.Aplicacion.Auth.Comandos.Login;

public class LoginHandler : IRequestHandler<LoginCommand, LoginResultDto>
{
    private readonly INeedlosDbContext _context;
    private readonly IJwtService _jwtService;
    private readonly IPasswordHasher _passwordHasher;

    public LoginHandler(INeedlosDbContext context, IJwtService jwtService, IPasswordHasher passwordHasher)
    {
        _context = context;
        _jwtService = jwtService;
        _passwordHasher = passwordHasher;
    }

    public async Task<LoginResultDto> Handle(LoginCommand request, CancellationToken cancellationToken)
    {
        var usuario = await _context.Usuarios
            .Include(u => u.Tenant)
            .Include(u => u.Roles).ThenInclude(ur => ur.Rol)
            .FirstOrDefaultAsync(u => u.Email == request.Email, cancellationToken);

        if (usuario is null || !_passwordHasher.Verify(request.Password, usuario.PasswordHash))
            throw new UnauthorizedAccessException("Credenciales inválidas.");

        if (!usuario.Activo)
            throw new UnauthorizedAccessException("Usuario inactivo.");

        var rol = usuario.Roles.FirstOrDefault()?.Rol?.Nombre ?? RolesConstantes.Admin;
        var accessToken = _jwtService.GenerarToken(usuario, rol);

        var refreshTokenRaw = TokenHasher.GenerarRaw();

        _context.RefreshTokens.Add(new RefreshToken
        {
            Id        = Guid.NewGuid(),
            UsuarioId = usuario.Id,
            TokenHash = TokenHasher.Hash(refreshTokenRaw),
            Expira    = DateTime.UtcNow.AddDays(7),
            CreadoEn  = DateTime.UtcNow
        });

        await _context.SaveChangesAsync(cancellationToken);

        return new LoginResultDto
        {
            AccessToken     = accessToken,
            RefreshTokenRaw = refreshTokenRaw,
            TenantId        = usuario.TenantId,
            Email           = usuario.Email
        };
    }
}
