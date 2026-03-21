namespace Needlos.Aplicacion.Auth.DTOs;

public class LoginResultDto
{
    public string Token { get; set; } = string.Empty;
    public Guid TenantId { get; set; }
    public string Email { get; set; } = string.Empty;
}
