using MediatR;
using Needlos.Aplicacion.Auth.DTOs;

namespace Needlos.Aplicacion.Auth.Comandos.Refresh;

public record RefrescarTokenCommand(string Token) : IRequest<RefrescarTokenResultDto>;
