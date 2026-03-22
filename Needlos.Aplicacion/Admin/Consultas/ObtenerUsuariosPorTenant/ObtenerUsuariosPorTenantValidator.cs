using FluentValidation;

namespace Needlos.Aplicacion.Admin.Consultas.ObtenerUsuariosPorTenant;

public class ObtenerUsuariosPorTenantValidator : AbstractValidator<ObtenerUsuariosPorTenantQuery>
{
    public ObtenerUsuariosPorTenantValidator()
    {
        RuleFor(x => x.TenantId)
            .NotEmpty().WithMessage("El tenantId es obligatorio.");

        RuleFor(x => x.Pagina)
            .GreaterThanOrEqualTo(1).WithMessage("La página debe ser mayor o igual a 1.");

        RuleFor(x => x.Tamano)
            .InclusiveBetween(1, 100).WithMessage("El tamaño de página debe estar entre 1 y 100.");
    }
}
