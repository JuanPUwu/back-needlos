using Needlos.Dominio.Enumeraciones;

namespace Needlos.Dominio.Entidades;

public class Pago : EntidadBase
{
    public Guid OrdenId { get; set; }
    public Orden? Orden { get; set; }

    public decimal Monto { get; set; }
    public MetodoPago Metodo { get; set; }
    public DateTime Fecha { get; set; } = DateTime.UtcNow;
}
