using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Needlos.Infraestructura.Migrations
{
    /// <inheritdoc />
    public partial class AgregarTelefonoYSuperAdminSemilla : Migration
    {
        // ID fijo del SuperAdmin semilla — nunca cambiar.
        private static readonly Guid _superAdminSemillaId  = new("00000000-0000-0000-0000-000000000004");
        private static readonly Guid _superAdminRolId      = new("00000000-0000-0000-0000-000000000002");
        private static readonly Guid _tenantSistemaId      = new("00000000-0000-0000-0000-000000000003");

        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            // ── Columna Telefono en Usuarios (nullable) ──────────────────────
            migrationBuilder.AddColumn<string>(
                name: "Telefono",
                table: "Usuarios",
                type: "text",
                nullable: true);

            // ── SuperAdmin semilla (email: "admin", password: "admin") ───────
            // Esta cuenta es fija del sistema y no puede eliminarse desde la app.
            // La contraseña es intencional para acceso de desarrollo; cámbiala en producción.
            var passwordHash = BCrypt.Net.BCrypt.HashPassword("admin", workFactor: 11);

            migrationBuilder.InsertData(
                table: "Usuarios",
                columns: new[] { "Id", "Email", "PasswordHash", "TenantId", "Telefono", "Activo" },
                values: new object[] { _superAdminSemillaId, "admin", passwordHash, _tenantSistemaId, null, true });

            migrationBuilder.InsertData(
                table: "UsuarioRoles",
                columns: new[] { "UsuarioId", "RolId" },
                values: new object[] { _superAdminSemillaId, _superAdminRolId });
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DeleteData(table: "UsuarioRoles", keyColumns: new[] { "UsuarioId", "RolId" },
                keyValues: new object[] { _superAdminSemillaId, _superAdminRolId });

            migrationBuilder.DeleteData(table: "Usuarios", keyColumn: "Id", keyValue: _superAdminSemillaId);

            migrationBuilder.DropColumn(
                name: "Telefono",
                table: "Usuarios");
        }
    }
}
