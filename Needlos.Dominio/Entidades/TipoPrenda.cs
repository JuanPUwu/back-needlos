namespace Needlos.Dominio.Entidades;

/// <summary>
/// Catálogo global de tipos de prenda predefinidos.
/// Entidad global: no tiene tenant, no hereda EntidadBase.
/// Los registros son semilla del sistema y no se crean desde la API.
/// </summary>
public class TipoPrenda
{
    public Guid   Id     { get; set; }
    public string Nombre { get; set; } = string.Empty;
}
