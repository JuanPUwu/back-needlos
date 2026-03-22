using MediatR;
using Needlos.Aplicacion.Admin.DTOs;
using Needlos.Aplicacion.Shared;

namespace Needlos.Aplicacion.Admin.Consultas.ObtenerTenants;

public record ObtenerTenantsQuery(int Pagina = 1, int Tamano = 20) : IRequest<PaginadoDto<TenantAdminDto>>;
