using MediatR;
using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.Excepciones;
using Needlos.Aplicacion.Shared;
using Needlos.Dominio.Entidades;

namespace Needlos.Aplicacion.Admin.Comandos.ConfigurarSuperAdmin;

public class ConfigurarSuperAdminHandler : IRequestHandler<ConfigurarSuperAdminCommand, Guid>
{
    private readonly INeedlosDbContext _context;
    private readonly IPasswordHasher _passwordHasher;

    public ConfigurarSuperAdminHandler(INeedlosDbContext context, IPasswordHasher passwordHasher)
    {
        _context = context;
        _passwordHasher = passwordHasher;
    }

    public async Task<Guid> Handle(ConfigurarSuperAdminCommand request, CancellationToken cancellationToken)
    {
        if (await _context.Usuarios.AnyAsync(u => u.Email == request.Email, cancellationToken))
            throw new ConflictException($"El email '{request.Email}' ya está registrado.");

        var usuario = new Usuario
        {
            Id           = Guid.NewGuid(),
            Email        = request.Email,
            PasswordHash = _passwordHasher.Hash(request.Password),
            TenantId     = RolesConstantes.TenantSistemaId,
            Telefono     = request.Telefono,
            Activo       = true
        };

        var usuarioRol = new UsuarioRol
        {
            UsuarioId = usuario.Id,
            RolId     = RolesConstantes.SuperAdminId
        };

        _context.Usuarios.Add(usuario);
        _context.UsuarioRoles.Add(usuarioRol);
        await _context.SaveChangesAsync(cancellationToken);

        return usuario.Id;
    }
}
