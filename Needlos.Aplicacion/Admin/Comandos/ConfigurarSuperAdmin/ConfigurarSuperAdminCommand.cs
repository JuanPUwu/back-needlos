using MediatR;

namespace Needlos.Aplicacion.Admin.Comandos.ConfigurarSuperAdmin;

/// <summary>
/// Crea un usuario SuperAdmin del sistema. Máximo 2 SuperAdmins permitidos:
/// si ya se alcanzó el límite, lanza ConflictException.
/// Endpoint público — no requiere autenticación.
/// </summary>
public record ConfigurarSuperAdminCommand(string Email, string Password, string Telefono) : IRequest<Guid>;
