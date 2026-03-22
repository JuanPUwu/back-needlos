using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Needlos.Infraestructura.Migrations
{
    /// <inheritdoc />
    public partial class TelefonoNotNullYActualizarSemilla : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            // Asignar el número de teléfono real al SuperAdmin semilla
            // y limpiar cualquier NULL residual antes de hacer la columna NOT NULL.
            migrationBuilder.Sql("""
                UPDATE "Usuarios"
                SET "Telefono" = '3133585900'
                WHERE "Id" = '00000000-0000-0000-0000-000000000004';

                UPDATE "Usuarios"
                SET "Telefono" = '0000000000'
                WHERE "Telefono" IS NULL;
                """);

            migrationBuilder.AlterColumn<string>(
                name: "Telefono",
                table: "Usuarios",
                type: "text",
                nullable: false,
                defaultValue: "",
                oldClrType: typeof(string),
                oldType: "text",
                oldNullable: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AlterColumn<string>(
                name: "Telefono",
                table: "Usuarios",
                type: "text",
                nullable: true,
                oldClrType: typeof(string),
                oldType: "text");
        }
    }
}
