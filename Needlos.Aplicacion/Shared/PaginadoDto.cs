namespace Needlos.Aplicacion.Shared;

/// <summary>
/// Wrapper genérico de respuesta paginada.
/// Todos los endpoints de lista devuelven este tipo en vez de List&lt;T&gt;.
///
/// Ejemplo de respuesta:
/// {
///   "datos": [...],
///   "pagina": 1,
///   "tamano": 20,
///   "total": 347,
///   "totalPaginas": 18
/// }
///
/// "totalPaginas" se calcula automáticamente y permite al frontend
/// saber cuántas páginas hay sin hacer una query adicional.
/// </summary>
public class PaginadoDto<T>
{
    public List<T> Datos { get; set; } = new();
    public int Pagina { get; set; }
    public int Tamano { get; set; }
    public int Total { get; set; }
    public int TotalPaginas => Tamano > 0 ? (int)Math.Ceiling(Total / (double)Tamano) : 0;
}
