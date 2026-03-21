namespace Needlos.Dominio.Entidades;

public class MedidasCliente : EntidadBase
{
    public Guid ClienteId { get; set; }
    public Cliente? Cliente { get; set; }

    public decimal Pecho { get; set; }
    public decimal Cintura { get; set; }
    public decimal Largo { get; set; }
    public string Observaciones { get; set; } = string.Empty;
}
