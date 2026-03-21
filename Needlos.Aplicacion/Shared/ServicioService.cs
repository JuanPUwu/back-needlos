using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.Excepciones;

namespace Needlos.Aplicacion.Shared;

/// <summary>
/// Servicio de aplicación con lógica reutilizable relacionada con servicios.
/// Se inyecta en cualquier handler que necesite verificar la existencia de un servicio,
/// centralizando la verificación en un solo lugar en lugar de repetir
/// la query en cada handler.
///
/// No es un repositorio: no abstrae EF Core, no tiene métodos CRUD.
/// Solo encapsula lógica de aplicación que varios handlers comparten.
/// </summary>
public class ServicioService(INeedlosDbContext context)
{
    /// <summary>
    /// Verifica que el servicio exista en el tenant actual.
    /// Lanza NotFoundException si no existe (el query filter de EF ya aplica el tenant).
    /// </summary>
    public async Task ValidarExistenciaAsync(Guid servicioId, CancellationToken cancellationToken)
    {
        if (!await context.Servicios.AnyAsync(s => s.Id == servicioId, cancellationToken))
            throw new NotFoundException($"Servicio '{servicioId}' no encontrado.");
    }
}
