using MediatR;
using Needlos.Dominio.Enumeraciones;

namespace Needlos.Aplicacion.Pagos.Comandos.CrearPago;

public record CrearPagoCommand(
    Guid OrdenId,
    decimal Monto,
    MetodoPago Metodo
) : IRequest<Guid>;
