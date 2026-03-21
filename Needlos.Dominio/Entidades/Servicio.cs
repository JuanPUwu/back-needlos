namespace Needlos.Dominio.Entidades;

public class Servicio : EntidadBase
{
    public string Nombre { get; set; } = string.Empty;
    public decimal PrecioBase { get; set; }
}
