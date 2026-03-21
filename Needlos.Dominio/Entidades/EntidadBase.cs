namespace Needlos.Dominio.Entidades;

public abstract class EntidadBase
{
    public Guid Id { get; set; }
    public Guid TenantId { get; set; }

    public bool Eliminado { get; set; }

    public DateTime CreadoEn { get; set; } = DateTime.UtcNow;
    public DateTime? ActualizadoEn { get; set; }

    public Guid CreadoPor { get; set; }
    public Guid? ActualizadoPor { get; set; }
}