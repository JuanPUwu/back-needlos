using MediatR;

namespace Needlos.Aplicacion.Auth.Comandos.Logout;

public record CerrarSesionCommand(string Token) : IRequest;
