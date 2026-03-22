using MediatR;
using Needlos.Aplicacion.Admin.DTOs;
using Needlos.Aplicacion.Shared;

namespace Needlos.Aplicacion.Admin.Consultas.ObtenerUsuariosPorTenant;

public record ObtenerUsuariosPorTenantQuery(Guid TenantId, int Pagina = 1, int Tamano = 20)
    : IRequest<PaginadoDto<UsuarioAdminDto>>;
