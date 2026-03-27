using Microsoft.EntityFrameworkCore;
using Needlos.Dominio.Entidades;

namespace Needlos.Aplicacion.Contratos;

public interface INeedlosDbContext
{
    DbSet<Orden>      Ordenes      { get; }
    DbSet<Prenda>     Prendas      { get; }
    DbSet<TipoPrenda> TiposPrendas { get; }
    DbSet<Cliente>    Clientes     { get; }
    DbSet<Pago>       Pagos        { get; }
    DbSet<Tenant>     Tenants      { get; }
    DbSet<Usuario>    Usuarios     { get; }
    DbSet<Rol>        Roles        { get; }
    DbSet<UsuarioRol>    UsuarioRoles  { get; }
    DbSet<RefreshToken>  RefreshTokens { get; }

    Task<int> SaveChangesAsync(CancellationToken cancellationToken = default);
}
