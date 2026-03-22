using System.Text.RegularExpressions;
using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Contratos;
using Needlos.Dominio.Entidades;
using Needlos.Infraestructura.Tenancy;

namespace Needlos.Infraestructura.Datos;

public class NeedlosDbContext : DbContext, INeedlosDbContext
{
    private readonly Guid _tenantId;
    private readonly Guid _usuarioId;

    // Campos que nunca se normalizan (sensibles o binarios)
    private static readonly HashSet<string> _camposExcluidos = new()
    {
        "PasswordHash"
    };

    public NeedlosDbContext(
        DbContextOptions<NeedlosDbContext> options,
        ITenantProvider tenantProvider
    ) : base(options)
    {
        _tenantId = tenantProvider.GetTenantId();
        _usuarioId = tenantProvider.GetUsuarioId();
    }

    // ── Entidades por tenant ──────────────────────────────────────
    public DbSet<Orden> Ordenes { get; set; }
    public DbSet<HistorialEstadoOrden> HistorialesEstadoOrden { get; set; }
    public DbSet<Cliente> Clientes { get; set; }
    public DbSet<Servicio> Servicios { get; set; }
    public DbSet<DetalleOrden> DetalleOrdenes { get; set; }
    public DbSet<MedidasCliente> MedidasClientes { get; set; }
    public DbSet<Pago> Pagos { get; set; }

    // ── Entidades globales (sin filtro de tenant) ─────────────────
    public DbSet<Tenant> Tenants { get; set; }
    public DbSet<Usuario> Usuarios { get; set; }
    public DbSet<Rol> Roles { get; set; }
    public DbSet<UsuarioRol> UsuarioRoles { get; set; }

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        // ── Filtros globales (tenant + soft delete) ───────────────
        modelBuilder.Entity<Orden>().HasQueryFilter(o => o.TenantId == _tenantId && !o.Eliminado);
        modelBuilder.Entity<HistorialEstadoOrden>().HasQueryFilter(h => h.TenantId == _tenantId && !h.Eliminado);
        modelBuilder.Entity<Cliente>().HasQueryFilter(c => c.TenantId == _tenantId && !c.Eliminado);
        modelBuilder.Entity<Servicio>().HasQueryFilter(s => s.TenantId == _tenantId && !s.Eliminado);
        modelBuilder.Entity<DetalleOrden>().HasQueryFilter(d => d.TenantId == _tenantId && !d.Eliminado);
        modelBuilder.Entity<MedidasCliente>().HasQueryFilter(m => m.TenantId == _tenantId && !m.Eliminado);
        modelBuilder.Entity<Pago>().HasQueryFilter(p => p.TenantId == _tenantId && !p.Eliminado);

        // ── Relaciones ────────────────────────────────────────────
        modelBuilder.Entity<Orden>()
            .HasOne(o => o.Cliente)
            .WithMany(c => c.Ordenes)
            .HasForeignKey(o => o.ClienteId)
            .OnDelete(DeleteBehavior.Restrict);

        modelBuilder.Entity<DetalleOrden>()
            .HasOne(d => d.Orden)
            .WithMany(o => o.Detalles)
            .HasForeignKey(d => d.OrdenId)
            .OnDelete(DeleteBehavior.Cascade);

        modelBuilder.Entity<DetalleOrden>()
            .HasOne(d => d.Servicio)
            .WithMany()
            .HasForeignKey(d => d.ServicioId)
            .OnDelete(DeleteBehavior.Restrict);

        modelBuilder.Entity<MedidasCliente>()
            .HasOne(m => m.Cliente)
            .WithMany(c => c.Medidas)
            .HasForeignKey(m => m.ClienteId)
            .OnDelete(DeleteBehavior.Cascade);

        modelBuilder.Entity<Pago>()
            .HasOne(p => p.Orden)
            .WithMany(o => o.Pagos)
            .HasForeignKey(p => p.OrdenId)
            .OnDelete(DeleteBehavior.Restrict);

        modelBuilder.Entity<HistorialEstadoOrden>()
            .HasOne(h => h.Orden)
            .WithMany(o => o.Historial)
            .HasForeignKey(h => h.OrdenId)
            .OnDelete(DeleteBehavior.Restrict);

        // ── Global: Usuario - Tenant ──────────────────────────────
        modelBuilder.Entity<Usuario>()
            .HasOne(u => u.Tenant)
            .WithMany()
            .HasForeignKey(u => u.TenantId)
            .OnDelete(DeleteBehavior.Restrict);

        // ── UsuarioRol (clave compuesta) ──────────────────────────
        modelBuilder.Entity<UsuarioRol>()
            .HasKey(ur => new { ur.UsuarioId, ur.RolId });

        modelBuilder.Entity<UsuarioRol>()
            .HasOne(ur => ur.Usuario)
            .WithMany(u => u.Roles)
            .HasForeignKey(ur => ur.UsuarioId);

        modelBuilder.Entity<UsuarioRol>()
            .HasOne(ur => ur.Rol)
            .WithMany()
            .HasForeignKey(ur => ur.RolId);

        // ── Índices únicos filtrados ──────────────────────────────
        // El filtro "Activo = true" evita que un registro inactivo/desactivado
        // bloquee la creación de uno nuevo con el mismo valor único.
        // Sin esto, el soft-disable de un tenant o usuario impediría reutilizar
        // el mismo slug o email aunque el registro ya no esté operativo.
        modelBuilder.Entity<Tenant>()
            .HasIndex(t => t.Slug)
            .IsUnique()
            .HasFilter("\"Activo\" = true");

        modelBuilder.Entity<Usuario>()
            .HasIndex(u => u.Email)
            .IsUnique()
            .HasFilter("\"Activo\" = true");

        // ── Conversión de enums a string ──────────────────────────
        modelBuilder.Entity<Orden>()
            .Property(o => o.Estado)
            .HasConversion<string>();

        modelBuilder.Entity<Pago>()
            .Property(p => p.Metodo)
            .HasConversion<string>();

        modelBuilder.Entity<HistorialEstadoOrden>()
            .Property(h => h.EstadoAnterior)
            .HasConversion<string>();

        modelBuilder.Entity<HistorialEstadoOrden>()
            .Property(h => h.EstadoNuevo)
            .HasConversion<string>();

        // ── Datos semilla (sistema) ───────────────────────────────
        // IDs fijos — nunca cambiar. Coinciden con RolesConstantes.
        modelBuilder.Entity<Rol>().HasData(
            new Rol { Id = new Guid("00000000-0000-0000-0000-000000000001"), Nombre = "SuperAdmin" },
            new Rol { Id = new Guid("00000000-0000-0000-0000-000000000002"), Nombre = "Admin" }
        );

        modelBuilder.Entity<Tenant>().HasData(new Tenant
        {
            Id      = new Guid("00000000-0000-0000-0000-000000000003"),
            Nombre  = "Sistema",
            Slug    = "sistema",
            Activo  = true,
            CreadoEn = new DateTime(2025, 1, 1, 0, 0, 0, DateTimeKind.Utc)
        });

        // SuperAdmin semilla: email=admin / password=admin
        modelBuilder.Entity<Usuario>().HasData(new Usuario
        {
            Id           = new Guid("00000000-0000-0000-0000-000000000004"),
            Email        = "admin",
            PasswordHash = "$2a$11$28e3BuDT5Q..C4crq9NVUuikukZGrdO0XOKwWTBKmxVmniCHtlC/6",
            TenantId     = new Guid("00000000-0000-0000-0000-000000000003"),
            Telefono     = "3133585900",
            Activo       = true
        });

        modelBuilder.Entity<UsuarioRol>().HasData(new UsuarioRol
        {
            UsuarioId = new Guid("00000000-0000-0000-0000-000000000004"),
            RolId     = new Guid("00000000-0000-0000-0000-000000000001")
        });
    }

    public override async Task<int> SaveChangesAsync(CancellationToken cancellationToken = default)
    {
        var entradas = ChangeTracker.Entries()
            .Where(e => e.State == EntityState.Added || e.State == EntityState.Modified);

        foreach (var entrada in entradas)
        {
            // ── Normalizar strings ────────────────────────────────
            foreach (var propiedad in entrada.Properties
                .Where(p => p.Metadata.ClrType == typeof(string)
                         && !_camposExcluidos.Contains(p.Metadata.Name)
                         && p.CurrentValue is string))
            {
                propiedad.CurrentValue = Normalizar((string)propiedad.CurrentValue!);
            }

            // ── Timestamps, TenantId y auditoría (solo EntidadBase) ──
            if (entrada.Entity is EntidadBase entidad)
            {
                switch (entrada.State)
                {
                    case EntityState.Added:
                        entidad.TenantId = _tenantId;
                        entidad.CreadoEn = DateTime.UtcNow;
                        entidad.CreadoPor = _usuarioId;
                        break;
                    case EntityState.Modified:
                        entidad.ActualizadoEn = DateTime.UtcNow;
                        entidad.ActualizadoPor = _usuarioId;
                        break;
                }
            }
        }

        return await base.SaveChangesAsync(cancellationToken);
    }

    // Trim + colapsar espacios internos + lowercase
    private static string Normalizar(string valor) =>
        Regex.Replace(valor.Trim(), @"\s+", " ").ToLowerInvariant();
}
