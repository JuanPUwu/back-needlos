namespace Needlos.Dominio.Entidades;

public class DetalleOrden : EntidadBase
{
    public Guid OrdenId { get; set; }
    public Orden? Orden { get; set; }

    public Guid ServicioId { get; set; }
    public Servicio? Servicio { get; set; }

    public decimal Precio { get; set; }
    public string Notas { get; set; } = string.Empty;
}
