using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Needlos.Infraestructura.Migrations
{
    /// <inheritdoc />
    public partial class AgregarAuditoriaYHistorialEstado : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropIndex(
                name: "IX_Usuarios_Email",
                table: "Usuarios");

            migrationBuilder.DropIndex(
                name: "IX_Tenants_Slug",
                table: "Tenants");

            migrationBuilder.AddColumn<Guid>(
                name: "ActualizadoPor",
                table: "Servicios",
                type: "uuid",
                nullable: true);

            migrationBuilder.AddColumn<Guid>(
                name: "CreadoPor",
                table: "Servicios",
                type: "uuid",
                nullable: false,
                defaultValue: new Guid("00000000-0000-0000-0000-000000000000"));

            migrationBuilder.AddColumn<Guid>(
                name: "ActualizadoPor",
                table: "Pagos",
                type: "uuid",
                nullable: true);

            migrationBuilder.AddColumn<Guid>(
                name: "CreadoPor",
                table: "Pagos",
                type: "uuid",
                nullable: false,
                defaultValue: new Guid("00000000-0000-0000-0000-000000000000"));

            migrationBuilder.AddColumn<Guid>(
                name: "ActualizadoPor",
                table: "Ordenes",
                type: "uuid",
                nullable: true);

            migrationBuilder.AddColumn<Guid>(
                name: "CreadoPor",
                table: "Ordenes",
                type: "uuid",
                nullable: false,
                defaultValue: new Guid("00000000-0000-0000-0000-000000000000"));

            migrationBuilder.AddColumn<Guid>(
                name: "ActualizadoPor",
                table: "MedidasClientes",
                type: "uuid",
                nullable: true);

            migrationBuilder.AddColumn<Guid>(
                name: "CreadoPor",
                table: "MedidasClientes",
                type: "uuid",
                nullable: false,
                defaultValue: new Guid("00000000-0000-0000-0000-000000000000"));

            migrationBuilder.AddColumn<Guid>(
                name: "ActualizadoPor",
                table: "DetalleOrdenes",
                type: "uuid",
                nullable: true);

            migrationBuilder.AddColumn<Guid>(
                name: "CreadoPor",
                table: "DetalleOrdenes",
                type: "uuid",
                nullable: false,
                defaultValue: new Guid("00000000-0000-0000-0000-000000000000"));

            migrationBuilder.AddColumn<Guid>(
                name: "ActualizadoPor",
                table: "Clientes",
                type: "uuid",
                nullable: true);

            migrationBuilder.AddColumn<Guid>(
                name: "CreadoPor",
                table: "Clientes",
                type: "uuid",
                nullable: false,
                defaultValue: new Guid("00000000-0000-0000-0000-000000000000"));

            migrationBuilder.CreateTable(
                name: "HistorialesEstadoOrden",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uuid", nullable: false),
                    OrdenId = table.Column<Guid>(type: "uuid", nullable: false),
                    EstadoAnterior = table.Column<string>(type: "text", nullable: false),
                    EstadoNuevo = table.Column<string>(type: "text", nullable: false),
                    TenantId = table.Column<Guid>(type: "uuid", nullable: false),
                    Eliminado = table.Column<bool>(type: "boolean", nullable: false),
                    CreadoEn = table.Column<DateTime>(type: "timestamp with time zone", nullable: false),
                    ActualizadoEn = table.Column<DateTime>(type: "timestamp with time zone", nullable: true),
                    CreadoPor = table.Column<Guid>(type: "uuid", nullable: false),
                    ActualizadoPor = table.Column<Guid>(type: "uuid", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_HistorialesEstadoOrden", x => x.Id);
                    table.ForeignKey(
                        name: "FK_HistorialesEstadoOrden_Ordenes_OrdenId",
                        column: x => x.OrdenId,
                        principalTable: "Ordenes",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Restrict);
                });

            migrationBuilder.CreateIndex(
                name: "IX_Usuarios_Email",
                table: "Usuarios",
                column: "Email",
                unique: true,
                filter: "\"Activo\" = true");

            migrationBuilder.CreateIndex(
                name: "IX_Tenants_Slug",
                table: "Tenants",
                column: "Slug",
                unique: true,
                filter: "\"Activo\" = true");

            migrationBuilder.CreateIndex(
                name: "IX_HistorialesEstadoOrden_OrdenId",
                table: "HistorialesEstadoOrden",
                column: "OrdenId");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "HistorialesEstadoOrden");

            migrationBuilder.DropIndex(
                name: "IX_Usuarios_Email",
                table: "Usuarios");

            migrationBuilder.DropIndex(
                name: "IX_Tenants_Slug",
                table: "Tenants");

            migrationBuilder.DropColumn(
                name: "ActualizadoPor",
                table: "Servicios");

            migrationBuilder.DropColumn(
                name: "CreadoPor",
                table: "Servicios");

            migrationBuilder.DropColumn(
                name: "ActualizadoPor",
                table: "Pagos");

            migrationBuilder.DropColumn(
                name: "CreadoPor",
                table: "Pagos");

            migrationBuilder.DropColumn(
                name: "ActualizadoPor",
                table: "Ordenes");

            migrationBuilder.DropColumn(
                name: "CreadoPor",
                table: "Ordenes");

            migrationBuilder.DropColumn(
                name: "ActualizadoPor",
                table: "MedidasClientes");

            migrationBuilder.DropColumn(
                name: "CreadoPor",
                table: "MedidasClientes");

            migrationBuilder.DropColumn(
                name: "ActualizadoPor",
                table: "DetalleOrdenes");

            migrationBuilder.DropColumn(
                name: "CreadoPor",
                table: "DetalleOrdenes");

            migrationBuilder.DropColumn(
                name: "ActualizadoPor",
                table: "Clientes");

            migrationBuilder.DropColumn(
                name: "CreadoPor",
                table: "Clientes");

            migrationBuilder.CreateIndex(
                name: "IX_Usuarios_Email",
                table: "Usuarios",
                column: "Email",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_Tenants_Slug",
                table: "Tenants",
                column: "Slug",
                unique: true);
        }
    }
}
