namespace Needlos.Aplicacion.Pagos.DTOs;

public class PagoDto
{
    public Guid Id { get; set; }
    public Guid OrdenId { get; set; }
    public decimal Monto { get; set; }
    public string Metodo { get; set; } = string.Empty;
    public DateTime Fecha { get; set; }
}
