using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.Excepciones;

namespace Needlos.Aplicacion.Shared;

/// <summary>
/// Servicio de aplicación con lógica reutilizable relacionada con órdenes.
/// Se inyecta en cualquier handler que necesite verificar o cruzar datos de órdenes
/// sin duplicar la misma query en múltiples handlers.
///
/// No es un repositorio: no abstrae EF Core, no tiene métodos CRUD.
/// Solo encapsula lógica de aplicación que varios handlers comparten.
/// </summary>
public class OrdenService(INeedlosDbContext context)
{
    /// <summary>
    /// Verifica que la orden exista en el tenant actual.
    /// Lanza NotFoundException si no existe (el query filter de EF ya aplica el tenant).
    /// </summary>
    public async Task ValidarExistenciaAsync(Guid ordenId, CancellationToken cancellationToken)
    {
        if (!await context.Ordenes.AnyAsync(o => o.Id == ordenId, cancellationToken))
            throw new NotFoundException($"Orden '{ordenId}' no encontrada.");
    }
}
