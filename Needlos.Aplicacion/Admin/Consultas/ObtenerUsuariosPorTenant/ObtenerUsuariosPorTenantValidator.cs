using FluentValidation;
using Needlos.Aplicacion.Shared;

namespace Needlos.Aplicacion.Admin.Consultas.ObtenerUsuariosPorTenant;

public class ObtenerUsuariosPorTenantValidator : AbstractValidator<ObtenerUsuariosPorTenantQuery>
{
    public ObtenerUsuariosPorTenantValidator()
    {
        RuleFor(x => x.TenantId)
            .NotEmpty().WithMessage("El tenantId es obligatorio.");

        this.ReglaPaginacion();
    }
}
