namespace Needlos.Aplicacion.Excepciones;

/// <summary>
/// Se lanza cuando un Command no supera las reglas de FluentValidation.
/// El ValidationBehavior la construye automáticamente antes de que llegue al handler.
/// El middleware la convierte en HTTP 400 con el array "errores".
/// </summary>
public class ValidationException(IEnumerable<string> errores)
    : Exception("Errores de validación")
{
    public IReadOnlyList<string> Errores { get; } = errores.ToList().AsReadOnly();
}
