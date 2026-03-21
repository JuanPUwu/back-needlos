namespace Needlos.Dominio.Excepciones;

/// <summary>
/// Excepción de dominio puro: representa una regla de negocio violada.
/// Vive en Dominio porque la origina la propia entidad, sin depender de nada externo.
/// El middleware la convierte en HTTP 400.
///
/// Ejemplos: intentar cambiar el estado de una orden ya entregada,
/// aplicar un descuento inválido, etc.
/// </summary>
public class BusinessException(string mensaje) : Exception(mensaje);
