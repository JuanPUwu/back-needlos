using MediatR;
using Needlos.Aplicacion.Auth.DTOs;

namespace Needlos.Aplicacion.Auth.Comandos.Login;

public record LoginCommand(string Email, string Password) : IRequest<LoginResultDto>;
