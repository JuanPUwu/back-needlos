namespace Needlos.Dominio.Entidades;

public class Usuario
{
    public Guid Id { get; set; }
    public string Email { get; set; } = string.Empty;
    public string PasswordHash { get; set; } = string.Empty;
    public Guid TenantId { get; set; }
    public bool Activo { get; set; } = true;

    public Tenant? Tenant { get; set; }
    public List<UsuarioRol> Roles { get; set; } = new();
}
