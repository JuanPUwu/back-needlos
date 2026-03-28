using System.Text.RegularExpressions;
using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Contratos;
using Needlos.Dominio.Entidades;
using Needlos.Dominio.Enumeraciones;
using Needlos.Infraestructura.Tenancy;

namespace Needlos.Infraestructura.Datos;

public class NeedlosDbContext : DbContext, INeedlosDbContext
{
    private readonly Guid _tenantId;
    private readonly Guid _usuarioId;

    // Campos que nunca se normalizan (sensibles o binarios)
    private static readonly HashSet<string> _camposExcluidos = new()
    {
        "PasswordHash",
        "TokenHash"       // SHA-256 hex — case-sensitive, no se normaliza
    };

    public NeedlosDbContext(
        DbContextOptions<NeedlosDbContext> options,
        ITenantProvider tenantProvider
    ) : base(options)
    {
        _tenantId  = tenantProvider.GetTenantId();
        _usuarioId = tenantProvider.GetUsuarioId();
    }

    // ── Entidades por tenant ──────────────────────────────────────
    public DbSet<Orden>   Ordenes  { get; set; }
    public DbSet<Prenda>  Prendas  { get; set; }
    public DbSet<Cliente> Clientes { get; set; }
    public DbSet<Pago>    Pagos    { get; set; }

    // ── Entidades globales (sin filtro de tenant) ─────────────────
    public DbSet<TipoPrenda>  TiposPrendas  { get; set; }
    public DbSet<Tenant>      Tenants       { get; set; }
    public DbSet<Usuario>     Usuarios      { get; set; }
    public DbSet<Rol>         Roles         { get; set; }
    public DbSet<UsuarioRol>  UsuarioRoles  { get; set; }
    public DbSet<RefreshToken> RefreshTokens { get; set; }

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        // ── Filtros globales (tenant + soft delete) ───────────────
        modelBuilder.Entity<Orden>()  .HasQueryFilter(o => o.TenantId == _tenantId && !o.Eliminado);
        modelBuilder.Entity<Prenda>() .HasQueryFilter(p => p.TenantId == _tenantId && !p.Eliminado);
        modelBuilder.Entity<Cliente>().HasQueryFilter(c => c.TenantId == _tenantId && !c.Eliminado);
        modelBuilder.Entity<Pago>()   .HasQueryFilter(p => p.TenantId == _tenantId && !p.Eliminado);

        // ── Relaciones ────────────────────────────────────────────
        modelBuilder.Entity<Orden>()
            .HasOne(o => o.Cliente)
            .WithMany(c => c.Ordenes)
            .HasForeignKey(o => o.ClienteId)
            .OnDelete(DeleteBehavior.Restrict);

        modelBuilder.Entity<Prenda>()
            .HasOne(p => p.Orden)
            .WithMany(o => o.Prendas)
            .HasForeignKey(p => p.OrdenId)
            .OnDelete(DeleteBehavior.Cascade);

        modelBuilder.Entity<Prenda>()
            .HasOne(p => p.TipoPrenda)
            .WithMany()
            .HasForeignKey(p => p.TipoPrendaId)
            .OnDelete(DeleteBehavior.Restrict);

        modelBuilder.Entity<Pago>()
            .HasOne(p => p.Orden)
            .WithMany(o => o.Pagos)
            .HasForeignKey(p => p.OrdenId)
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

        // ── RefreshToken ──────────────────────────────────────────
        modelBuilder.Entity<RefreshToken>()
            .HasOne(rt => rt.Usuario)
            .WithMany()
            .HasForeignKey(rt => rt.UsuarioId)
            .OnDelete(DeleteBehavior.Cascade);

        modelBuilder.Entity<RefreshToken>()
            .HasIndex(rt => rt.TokenHash)
            .IsUnique();

        // ── Índices únicos filtrados ──────────────────────────────
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
            .Property(o => o.TipoOrden)
            .HasConversion<string>();

        modelBuilder.Entity<Prenda>()
            .Property(p => p.Estado)
            .HasConversion<string>();

        modelBuilder.Entity<Pago>()
            .Property(p => p.Metodo)
            .HasConversion<string>();

        // ── Datos semilla (sistema) ───────────────────────────────
        // IDs fijos — nunca cambiar. Coinciden con RolesConstantes.
        modelBuilder.Entity<Rol>().HasData(
            new Rol { Id = new Guid("00000000-0000-0000-0000-000000000001"), Nombre = "SuperAdmin" },
            new Rol { Id = new Guid("00000000-0000-0000-0000-000000000002"), Nombre = "Admin" }
        );

        modelBuilder.Entity<Tenant>().HasData(new Tenant
        {
            Id       = new Guid("00000000-0000-0000-0000-000000000003"),
            Nombre   = "Sistema",
            Slug     = "sistema",
            Activo   = true,
            CreadoEn = new DateTime(2025, 1, 1, 0, 0, 0, DateTimeKind.Utc)
        });

        // SuperAdmin semilla: email=admin@example.com / password=admin
        modelBuilder.Entity<Usuario>().HasData(new Usuario
        {
            Id           = new Guid("00000000-0000-0000-0000-000000000004"),
            Email        = "admin@example.com",
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

        // ── Semilla de TiposPrendas (IDs fijos) ───────────────────
        modelBuilder.Entity<TipoPrenda>().HasData(
            new TipoPrenda { Id = new Guid("00000000-0000-0000-0001-000000000001"), Nombre = "Pantalón" },
            new TipoPrenda { Id = new Guid("00000000-0000-0000-0001-000000000002"), Nombre = "Camisa" },
            new TipoPrenda { Id = new Guid("00000000-0000-0000-0001-000000000003"), Nombre = "Vestido" },
            new TipoPrenda { Id = new Guid("00000000-0000-0000-0001-000000000004"), Nombre = "Saco" },
            new TipoPrenda { Id = new Guid("00000000-0000-0000-0001-000000000005"), Nombre = "Traje completo" },
            new TipoPrenda { Id = new Guid("00000000-0000-0000-0001-000000000006"), Nombre = "Falda" },
            new TipoPrenda { Id = new Guid("00000000-0000-0000-0001-000000000007"), Nombre = "Chaqueta" },
            new TipoPrenda { Id = new Guid("00000000-0000-0000-0001-000000000008"), Nombre = "Chaleco" },
            new TipoPrenda { Id = new Guid("00000000-0000-0000-0001-000000000009"), Nombre = "Corbata" },
            new TipoPrenda { Id = new Guid("00000000-0000-0000-0001-000000000010"), Nombre = "Abrigo" }
        );
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
                        entidad.TenantId  = _tenantId;
                        entidad.CreadoEn  = DateTime.UtcNow;
                        entidad.CreadoPor = _usuarioId;
                        break;
                    case EntityState.Modified:
                        entidad.ActualizadoEn  = DateTime.UtcNow;
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
