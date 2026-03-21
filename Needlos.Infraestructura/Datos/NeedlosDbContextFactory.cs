using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Design;
using Needlos.Aplicacion.Contratos;

namespace Needlos.Infraestructura.Datos;

public class NeedlosDbContextFactory : IDesignTimeDbContextFactory<NeedlosDbContext>
{
    public NeedlosDbContext CreateDbContext(string[] args)
    {
        var optionsBuilder = new DbContextOptionsBuilder<NeedlosDbContext>();

        optionsBuilder.UseNpgsql("Host=localhost;Port=5432;Database=needlos_db;Username=postgres;Password=admin");

        return new NeedlosDbContext(optionsBuilder.Options, new DesignTimeTenantProvider());
    }

    private sealed class DesignTimeTenantProvider : ITenantProvider
    {
        public Guid GetTenantId() => Guid.Empty;
        public Guid GetUsuarioId() => Guid.Empty;
    }
}
