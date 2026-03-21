using Needlos.Dominio.Enumeraciones;
using Needlos.Dominio.Excepciones;

namespace Needlos.Dominio.Entidades;

public class Orden : EntidadBase
{
    public Guid ClienteId { get; set; }
    public Cliente? Cliente { get; set; }

    public EstadoOrden Estado { get; set; } = EstadoOrden.Pendiente;
    public decimal PrecioTotal { get; set; }
    public DateTime FechaEntrega { get; set; }

    public List<DetalleOrden> Detalles { get; set; } = new();
    public List<Pago> Pagos { get; set; } = new();
    public List<HistorialEstadoOrden> Historial { get; set; } = new();

    /// <summary>
    /// Cambia el estado de la orden aplicando las reglas de negocio del dominio.
    /// Esta lógica vive en la entidad (no en el handler) porque es una invariante
    /// del negocio: la entidad es responsable de proteger su propio estado.
    ///
    /// Regla actual: una orden Entregada es final y no puede modificarse.
    /// Lanza BusinessException si la transición no es permitida.
    /// </summary>
    public void CambiarEstado(EstadoOrden nuevoEstado)
    {
        if (Estado == EstadoOrden.Entregado)
            throw new BusinessException(
                $"La orden ya fue entregada y no puede cambiar a '{nuevoEstado}'.");

        Estado = nuevoEstado;
    }
}
