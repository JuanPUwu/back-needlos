using System.Globalization;
using System.Text;
using System.Text.RegularExpressions;
using MediatR;
using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.Excepciones;
using Needlos.Aplicacion.Shared;
using Needlos.Dominio.Entidades;

namespace Needlos.Aplicacion.Auth.Comandos.Registrar;

public class RegistrarTenantHandler : IRequestHandler<RegistrarTenantCommand, Guid>
{
    private readonly INeedlosDbContext _context;
    private readonly IPasswordHasher _passwordHasher;

    public RegistrarTenantHandler(INeedlosDbContext context, IPasswordHasher passwordHasher)
    {
        _context = context;
        _passwordHasher = passwordHasher;
    }

    public async Task<Guid> Handle(RegistrarTenantCommand request, CancellationToken cancellationToken)
    {
        if (await _context.Usuarios.AnyAsync(u => u.Email == request.Email, cancellationToken))
            throw new ConflictException($"El email '{request.Email}' ya está registrado.");

        var slug = await GenerarSlugUnico(request.NombreTienda, cancellationToken);

        var tenant = new Tenant
        {
            Id = Guid.NewGuid(),
            Nombre = request.NombreTienda,
            Slug = slug,
            Activo = true,
            CreadoEn = DateTime.UtcNow
        };

        var usuario = new Usuario
        {
            Id           = Guid.NewGuid(),
            Email        = request.Email,
            PasswordHash = _passwordHasher.Hash(request.Password),
            TenantId     = tenant.Id,
            Telefono     = request.Telefono,
            Activo       = true
        };

        var usuarioRol = new UsuarioRol
        {
            UsuarioId = usuario.Id,
            RolId     = RolesConstantes.AdminId
        };

        _context.Tenants.Add(tenant);
        _context.Usuarios.Add(usuario);
        _context.UsuarioRoles.Add(usuarioRol);
        await _context.SaveChangesAsync(cancellationToken);

        return tenant.Id;
    }

    // Genera un slug único: "Sastrería Juan" → "sastreria-juan"
    // Si ya existe, agrega sufijo numérico: "sastreria-juan-2", "sastreria-juan-3"...
    private async Task<string> GenerarSlugUnico(string nombre, CancellationToken cancellationToken)
    {
        var slugBase = GenerarSlug(nombre);

        if (!await _context.Tenants.AnyAsync(t => t.Slug == slugBase, cancellationToken))
            return slugBase;

        var sufijo = 2;
        string slug;
        do
        {
            slug = $"{slugBase}-{sufijo++}";
        }
        while (await _context.Tenants.AnyAsync(t => t.Slug == slug, cancellationToken));

        return slug;
    }

    private static string GenerarSlug(string nombre)
    {
        // Separar caracteres base de diacríticos (á→a, é→e, ñ→n, ü→u...)
        var normalizado = nombre.Normalize(NormalizationForm.FormD);
        var sinAcentos = new string(
            normalizado.Where(c =>
                CharUnicodeInfo.GetUnicodeCategory(c) != UnicodeCategory.NonSpacingMark
            ).ToArray()
        );

        // Lowercase → espacios a guion → quitar cualquier carácter no permitido → trim guiones
        return Regex.Replace(
                sinAcentos.ToLowerInvariant().Replace(' ', '-'),
                @"[^a-z0-9\-]", "")
            .Trim('-');
    }
}
