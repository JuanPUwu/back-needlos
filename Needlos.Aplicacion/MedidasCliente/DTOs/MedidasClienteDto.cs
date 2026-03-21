namespace Needlos.Aplicacion.MedidasCliente.DTOs;

public class MedidasClienteDto
{
    public Guid Id { get; set; }
    public Guid ClienteId { get; set; }
    public decimal Pecho { get; set; }
    public decimal Cintura { get; set; }
    public decimal Largo { get; set; }
    public string Observaciones { get; set; } = string.Empty;
}
