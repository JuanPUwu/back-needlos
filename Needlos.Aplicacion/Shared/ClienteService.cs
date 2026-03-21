using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Contratos;
using Needlos.Aplicacion.Excepciones;

namespace Needlos.Aplicacion.Shared;

/// <summary>
/// Servicio de aplicación con lógica reutilizable relacionada con clientes.
/// Se inyecta en cualquier handler que necesite operar sobre clientes,
/// centralizando la verificación en un solo lugar en lugar de repetir
/// la query en cada handler.
///
/// No es un repositorio: no abstrae EF Core, no tiene métodos CRUD.
/// Solo encapsula lógica de aplicación que varios handlers comparten.
/// </summary>
public class ClienteService(INeedlosDbContext context)
{
    /// <summary>
    /// Verifica que el cliente exista en el tenant actual.
    /// Lanza NotFoundException si no existe (el query filter de EF ya aplica el tenant).
    /// </summary>
    public async Task ValidarExistenciaAsync(Guid clienteId, CancellationToken cancellationToken)
    {
        if (!await context.Clientes.AnyAsync(c => c.Id == clienteId, cancellationToken))
            throw new NotFoundException($"Cliente '{clienteId}' no encontrado.");
    }
}
