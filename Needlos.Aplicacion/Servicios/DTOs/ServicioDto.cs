namespace Needlos.Aplicacion.Servicios.DTOs;

public class ServicioDto
{
    public Guid Id { get; set; }
    public string Nombre { get; set; } = string.Empty;
    public decimal PrecioBase { get; set; }
}
