namespace Needlos.Aplicacion.Admin.DTOs;

public class UsuarioAdminDto
{
    public Guid Id { get; set; }
    public string Email { get; set; } = string.Empty;
    public bool Activo { get; set; }
    public List<string> Roles { get; set; } = new();
}
