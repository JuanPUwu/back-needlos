namespace Needlos.Aplicacion.Ordenes.DTOs;

public class OrdenDto
{
    public Guid Id { get; set; }
    public Guid ClienteId { get; set; }
    public string NombreCliente { get; set; } = string.Empty;
    public string Estado { get; set; } = string.Empty;
    public decimal PrecioTotal { get; set; }
    public DateTime FechaEntrega { get; set; }
    public DateTime CreadoEn { get; set; }
    public List<DetalleOrdenDto> Detalles { get; set; } = new();
}

public class DetalleOrdenDto
{
    public Guid Id { get; set; }
    public Guid ServicioId { get; set; }
    public string NombreServicio { get; set; } = string.Empty;
    public decimal Precio { get; set; }
    public string Notas { get; set; } = string.Empty;
}
