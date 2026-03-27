namespace Needlos.Aplicacion.Ordenes.DTOs;

public class OrdenDto
{
    public Guid     Id             { get; set; }
    public Guid     ClienteId      { get; set; }
    public string   NombreCliente  { get; set; } = string.Empty;
    public string   ApellidoCliente { get; set; } = string.Empty;
    public string   TipoOrden      { get; set; } = string.Empty;

    // Campos derivados de las prendas
    public string   Estado         { get; set; } = string.Empty;
    public decimal  PrecioTotal    { get; set; }
    public DateOnly FechaEntrega   { get; set; }

    public DateTime CreadoEn { get; set; }
    public List<PrendaDto> Prendas { get; set; } = new();
}

public class PrendaDto
{
    public Guid     Id             { get; set; }
    public Guid     TipoPrendaId   { get; set; }
    public string   TipoPrenda     { get; set; } = string.Empty;
    public int      Cantidad       { get; set; }
    public string   Descripcion    { get; set; } = string.Empty;
    public decimal  PrecioPorUnidad { get; set; }
    public decimal  PrecioTotal    { get; set; }
    public DateOnly FechaEntrega   { get; set; }
    public string   Estado         { get; set; } = string.Empty;
}
