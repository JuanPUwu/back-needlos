namespace Needlos.Aplicacion.Clientes.DTOs;

public class ClienteDto
{
    public Guid     Id            { get; set; }
    public string   Nombre        { get; set; } = string.Empty;
    public string   Apellido      { get; set; } = string.Empty;
    public string   Telefono      { get; set; } = string.Empty;
    public DateTime FechaRegistro { get; set; }
}
