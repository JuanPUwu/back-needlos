namespace Needlos.Dominio.Entidades;

public class Cliente : EntidadBase
{
    public string Nombre { get; set; } = string.Empty;
    public string Telefono { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public DateTime FechaRegistro { get; set; } = DateTime.UtcNow;

    public List<Orden> Ordenes { get; set; } = new();
    public List<MedidasCliente> Medidas { get; set; } = new();
}
