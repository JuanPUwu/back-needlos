namespace Needlos.Aplicacion.Shared;

/// <summary>
/// IDs y nombres de roles del sistema. Son constantes fijas sembradas en BD por migración.
/// Los nombres deben coincidir exactamente con los valores en la tabla Roles
/// y con los atributos [Authorize(Roles = "...")] de los controllers.
/// </summary>
public static class RolesConstantes
{
    public const string Admin      = "Admin";
    public const string SuperAdmin = "SuperAdmin";

    public static readonly Guid SuperAdminId = new("00000000-0000-0000-0000-000000000001");
    public static readonly Guid AdminId      = new("00000000-0000-0000-0000-000000000002");

    /// <summary>Tenant especial del sistema. Se usa como TenantId del usuario SuperAdmin.</summary>
    public static readonly Guid TenantSistemaId = new("00000000-0000-0000-0000-000000000003");
}
