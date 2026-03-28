using System.Data;
using Microsoft.EntityFrameworkCore;
using Needlos.Aplicacion.Admin.DTOs;
using Needlos.Aplicacion.Contratos;
using Needlos.Infraestructura.Datos;

namespace Needlos.Infraestructura.Estadisticas;

public class EstadisticasBdService(NeedlosDbContext context) : IEstadisticasBdService
{
    public async Task<EstadisticasBdDto> ObtenerEstadisticasAsync(CancellationToken ct = default)
    {
        var conn    = context.Database.GetDbConnection();
        var wasOpen = conn.State == ConnectionState.Open;
        if (!wasOpen) await conn.OpenAsync(ct);

        try
        {
            // ── 1. Tamaño total de la base de datos ───────────────────────
            long   tamanoBdBytes;
            string tamanoBd;

            using (var cmd = conn.CreateCommand())
            {
                cmd.CommandText =
                    "SELECT pg_database_size(current_database()), " +
                    "pg_size_pretty(pg_database_size(current_database()))";

                await using var reader = await cmd.ExecuteReaderAsync(ct);
                await reader.ReadAsync(ct);
                tamanoBdBytes = reader.GetInt64(0);
                tamanoBd      = reader.GetString(1);
            }

            // ── 2. Estadísticas por tabla ─────────────────────────────────
            var tablas = new List<EstadisticaTablaDto>();

            using (var cmd = conn.CreateCommand())
            {
                cmd.CommandText = @"
                    SELECT
                        c.relname,
                        COALESCE(s.n_live_tup, 0),
                        pg_total_relation_size(c.oid),
                        pg_size_pretty(pg_total_relation_size(c.oid)),
                        pg_relation_size(c.oid),
                        pg_size_pretty(pg_relation_size(c.oid)),
                        pg_total_relation_size(c.oid) - pg_relation_size(c.oid),
                        pg_size_pretty(pg_total_relation_size(c.oid) - pg_relation_size(c.oid))
                    FROM pg_class c
                    JOIN pg_namespace n ON n.oid = c.relnamespace
                    LEFT JOIN pg_stat_user_tables s
                        ON s.relname = c.relname AND s.schemaname = 'public'
                    WHERE c.relkind = 'r' AND n.nspname = 'public'
                    ORDER BY pg_total_relation_size(c.oid) DESC";

                await using var reader = await cmd.ExecuteReaderAsync(ct);
                while (await reader.ReadAsync(ct))
                {
                    tablas.Add(new EstadisticaTablaDto(
                        Tabla:              reader.GetString(0),
                        FilasEstimadas:     reader.GetInt64(1),
                        TamanoTotalBytes:   reader.GetInt64(2),
                        TamanoTotal:        reader.GetString(3),
                        TamanoDatosBytes:   reader.GetInt64(4),
                        TamanoDatos:        reader.GetString(5),
                        TamanoIndicesBytes: reader.GetInt64(6),
                        TamanoIndices:      reader.GetString(7)
                    ));
                }
            }

            return new EstadisticasBdDto(tamanoBdBytes, tamanoBd, tablas);
        }
        finally
        {
            if (!wasOpen) await conn.CloseAsync();
        }
    }
}
