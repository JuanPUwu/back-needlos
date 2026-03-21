using MediatR;

namespace Needlos.Aplicacion.Auth.Comandos.Registrar;

public record RegistrarTenantCommand(
    string NombreTienda,
    string Email,
    string Password
) : IRequest<Guid>;
