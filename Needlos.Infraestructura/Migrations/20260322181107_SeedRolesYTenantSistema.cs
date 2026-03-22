using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Needlos.Infraestructura.Migrations
{
    /// <inheritdoc />
    public partial class SeedRolesYTenantSistema : Migration
    {
        // IDs fijos del sistema — deben coincidir con RolesConstantes en Aplicacion.
        private static readonly Guid _adminRolId      = new("00000000-0000-0000-0000-000000000001");
        private static readonly Guid _superAdminRolId = new("00000000-0000-0000-0000-000000000002");
        private static readonly Guid _tenantSistemaId = new("00000000-0000-0000-0000-000000000003");

        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            // ── Roles del sistema ────────────────────────────────────────────
            migrationBuilder.InsertData(
                table: "Roles",
                columns: new[] { "Id", "Nombre" },
                values: new object[] { _adminRolId, "Admin" });

            migrationBuilder.InsertData(
                table: "Roles",
                columns: new[] { "Id", "Nombre" },
                values: new object[] { _superAdminRolId, "SuperAdmin" });

            // ── Tenant especial del sistema (TenantId del SuperAdmin) ────────
            // El slug "sistema" está reservado y no puede ser tomado por ninguna sastrería.
            migrationBuilder.InsertData(
                table: "Tenants",
                columns: new[] { "Id", "Nombre", "Slug", "Activo", "CreadoEn" },
                values: new object[] { _tenantSistemaId, "Sistema NeedlOS", "sistema", true, DateTime.UtcNow });
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DeleteData(table: "Tenants", keyColumn: "Id", keyValue: _tenantSistemaId);
            migrationBuilder.DeleteData(table: "Roles",   keyColumn: "Id", keyValue: _superAdminRolId);
            migrationBuilder.DeleteData(table: "Roles",   keyColumn: "Id", keyValue: _adminRolId);
        }
    }
}
