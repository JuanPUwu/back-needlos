using Needlos.Dominio.Enumeraciones;
using Needlos.Dominio.Excepciones;

namespace Needlos.Dominio.Entidades;

public class Prenda : EntidadBase
{
    public Guid   OrdenId      { get; set; }
    public Orden? Orden        { get; set; }

    public Guid        TipoPrendaId { get; set; }
    public TipoPrenda? TipoPrenda   { get; set; }

    public int     Cantidad        { get; set; }
    public string  Descripcion     { get; set; } = string.Empty;
    public decimal PrecioPorUnidad { get; set; }
    public DateOnly FechaEntrega   { get; set; }

    public EstadoPrenda Estado { get; set; } = EstadoPrenda.EnProceso;

    /// <summary>
    /// Cambia el estado de la prenda.
    /// Regla: una prenda Entregada es final y no puede modificarse.
    /// </summary>
    public void CambiarEstado(EstadoPrenda nuevoEstado)
    {
        if (Estado == EstadoPrenda.Entregado)
            throw new BusinessException(
                $"La prenda ya fue entregada y no puede cambiar de estado.");

        Estado = nuevoEstado;
    }
}
