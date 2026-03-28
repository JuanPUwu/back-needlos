using DotNetEnv;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Design;
using Needlos.Aplicacion.Contratos;

namespace Needlos.Infraestructura.Datos;

public class NeedlosDbContextFactory : IDesignTimeDbContextFactory<NeedlosDbContext>
{
    public NeedlosDbContext CreateDbContext(string[] args)
    {
        // Cargar .env para que las migraciones usen la misma connection string
        // que la aplicación, sin hardcodear nada aquí.
        Env.NoClobber().TraversePath().Load();

        var connectionString = Environment.GetEnvironmentVariable("ConnectionStrings__DefaultConnection")
            ?? throw new InvalidOperationException(
                "La variable 'ConnectionStrings__DefaultConnection' no está definida. " +
                "Asegúrate de tener un archivo .env en la raíz del proyecto.");

        var optionsBuilder = new DbContextOptionsBuilder<NeedlosDbContext>();
        optionsBuilder.UseNpgsql(connectionString);

        return new NeedlosDbContext(optionsBuilder.Options, new DesignTimeTenantProvider());
    }

    private sealed class DesignTimeTenantProvider : ITenantProvider
    {
        public Guid GetTenantId() => Guid.Empty;
        public Guid GetUsuarioId() => Guid.Empty;
    }
}
