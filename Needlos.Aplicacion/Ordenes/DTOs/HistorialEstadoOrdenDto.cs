namespace Needlos.Aplicacion.Ordenes.DTOs;

public class HistorialEstadoOrdenDto
{
    public Guid Id { get; set; }
    public string EstadoAnterior { get; set; } = string.Empty;
    public string EstadoNuevo { get; set; } = string.Empty;
    public Guid CambiadoPor { get; set; }
    public DateTime CambiadoEn { get; set; }
}
