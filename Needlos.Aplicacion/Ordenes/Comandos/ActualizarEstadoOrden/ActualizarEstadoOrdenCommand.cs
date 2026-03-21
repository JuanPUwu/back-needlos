using MediatR;
using Needlos.Dominio.Enumeraciones;

namespace Needlos.Aplicacion.Ordenes.Comandos.ActualizarEstadoOrden;

public record ActualizarEstadoOrdenCommand(Guid OrdenId, EstadoOrden NuevoEstado) : IRequest<Unit>;
