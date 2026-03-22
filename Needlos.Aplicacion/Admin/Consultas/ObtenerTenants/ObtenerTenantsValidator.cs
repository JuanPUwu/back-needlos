using FluentValidation;

namespace Needlos.Aplicacion.Admin.Consultas.ObtenerTenants;

public class ObtenerTenantsValidator : AbstractValidator<ObtenerTenantsQuery>
{
    public ObtenerTenantsValidator()
    {
        RuleFor(x => x.Pagina)
            .GreaterThanOrEqualTo(1).WithMessage("La página debe ser mayor o igual a 1.");

        RuleFor(x => x.Tamano)
            .InclusiveBetween(1, 100).WithMessage("El tamaño de página debe estar entre 1 y 100.");
    }
}
