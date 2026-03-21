using FluentValidation;

namespace Needlos.Aplicacion.Servicios.Consultas.ObtenerServicios;

public class ObtenerServiciosValidator : AbstractValidator<ObtenerServiciosQuery>
{
    public ObtenerServiciosValidator()
    {
        RuleFor(x => x.Pagina)
            .GreaterThanOrEqualTo(1).WithMessage("La página debe ser mayor o igual a 1.");

        RuleFor(x => x.Tamano)
            .InclusiveBetween(1, 100).WithMessage("El tamaño de página debe estar entre 1 y 100.");
    }
}
