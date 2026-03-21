using Needlos.Dominio.Enumeraciones;

namespace Needlos.Dominio.Entidades;

/// <summary>
/// Registro inmutable de cada transición de estado de una orden.
/// CreadoEn y CreadoPor (heredados de EntidadBase) indican cuándo y quién realizó el cambio.
/// </summary>
public class HistorialEstadoOrden : EntidadBase
{
    public Guid OrdenId { get; set; }
    public Orden? Orden { get; set; }

    public EstadoOrden EstadoAnterior { get; set; }
    public EstadoOrden EstadoNuevo { get; set; }
}
