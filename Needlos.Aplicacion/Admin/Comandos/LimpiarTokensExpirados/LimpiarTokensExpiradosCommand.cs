using MediatR;

namespace Needlos.Aplicacion.Admin.Comandos.LimpiarTokensExpirados;

/// <summary>Elimina físicamente todos los refresh tokens expirados de la BD.</summary>
/// <returns>Número de tokens eliminados.</returns>
public record LimpiarTokensExpiradosCommand : IRequest<int>;
