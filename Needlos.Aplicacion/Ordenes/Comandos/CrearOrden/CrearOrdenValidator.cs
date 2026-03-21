using FluentValidation;

namespace Needlos.Aplicacion.Ordenes.Comandos.CrearOrden;

public class CrearOrdenValidator : AbstractValidator<CrearOrdenCommand>
{
    public CrearOrdenValidator()
    {
        RuleFor(x => x.ClienteId)
            .NotEmpty().WithMessage("El id del cliente es obligatorio.");

        RuleFor(x => x.FechaEntrega)
            .GreaterThan(DateTime.UtcNow).WithMessage("La fecha de entrega debe ser futura.");

        RuleFor(x => x.Detalles)
            .NotEmpty().WithMessage("La orden debe tener al menos un detalle.");

        RuleForEach(x => x.Detalles).ChildRules(detalle =>
        {
            detalle.RuleFor(d => d.ServicioId)
                .NotEmpty().WithMessage("El id del servicio es obligatorio en cada detalle.");

            detalle.RuleFor(d => d.Precio)
                .GreaterThan(0).WithMessage("El precio de cada detalle debe ser mayor a 0.");
        });
    }
}
