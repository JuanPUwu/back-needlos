namespace Needlos.Aplicacion.Admin.DTOs;

public class TenantAdminDto
{
    public Guid Id { get; set; }
    public string Nombre { get; set; } = string.Empty;
    public string Slug { get; set; } = string.Empty;
    public bool Activo { get; set; }
    public DateTime CreadoEn { get; set; }
}
