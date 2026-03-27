namespace Needlos.Aplicacion.Auth.DTOs;

public class LoginResultDto
{
    public string AccessToken { get; set; } = string.Empty;
    /// <summary>Token raw sin hashear — solo para que el controller lo coloque en cookie HttpOnly.</summary>
    public string RefreshTokenRaw { get; set; } = string.Empty;
    public Guid TenantId { get; set; }
    public string Email { get; set; } = string.Empty;
}
