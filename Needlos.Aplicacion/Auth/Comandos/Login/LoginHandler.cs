using MediatR;
using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Auth.DTOs;
using Needlos.Aplicacion.Contratos;

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
            .FirstOrDefaultAsync(u => u.Email == request.Email, cancellationToken);

        if (usuario is null || !_passwordHasher.Verify(request.Password, usuario.PasswordHash))
            throw new UnauthorizedAccessException("Credenciales inválidas.");

        if (!usuario.Activo)
            throw new UnauthorizedAccessException("Usuario inactivo.");

        var token = _jwtService.GenerarToken(usuario, "Admin");

        return new LoginResultDto
        {
            Token = token,
            TenantId = usuario.TenantId,
            Email = usuario.Email
        };
    }
}
