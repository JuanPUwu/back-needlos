namespace Needlos.Dominio.Entidades;

public class Cliente : EntidadBase
{
    public string Nombre   { get; set; } = string.Empty;
    public string Apellido { get; set; } = string.Empty;
    public string Telefono { get; set; } = string.Empty;

    public DateTime FechaRegistro { get; set; } = DateTime.UtcNow;

    public List<Orden> Ordenes { get; set; } = new();
}
