namespace Needlos.Aplicacion.Auth.DTOs;

public class RefrescarTokenResultDto
{
    public string AccessToken { get; set; } = string.Empty;
    /// <summary>Token raw sin hashear — solo para que el controller lo rote en cookie HttpOnly.</summary>
    public string RefreshTokenRaw { get; set; } = string.Empty;
}
