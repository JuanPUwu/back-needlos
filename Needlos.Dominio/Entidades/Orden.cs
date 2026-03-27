using Needlos.Dominio.Enumeraciones;

namespace Needlos.Dominio.Entidades;

public class Orden : EntidadBase
{
    public Guid     ClienteId { get; set; }
    public Cliente? Cliente   { get; set; }

    public TipoOrden TipoOrden { get; set; } = TipoOrden.Arreglo;

    public List<Prenda> Prendas { get; set; } = new();
    public List<Pago>   Pagos   { get; set; } = new();
}
