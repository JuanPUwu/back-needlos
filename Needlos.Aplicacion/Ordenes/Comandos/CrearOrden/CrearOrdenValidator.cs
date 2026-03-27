using FluentValidation;

namespace Needlos.Aplicacion.Ordenes.Comandos.CrearOrden;

public class CrearOrdenValidator : AbstractValidator<CrearOrdenCommand>
{
    public CrearOrdenValidator()
    {
        RuleFor(x => x.ClienteId)
            .NotEmpty().WithMessage("El cliente es obligatorio.");

        RuleFor(x => x.Prendas)
            .NotEmpty().WithMessage("La orden debe tener al menos una prenda.");

        RuleForEach(x => x.Prendas).ChildRules(prenda =>
        {
            prenda.RuleFor(p => p.TipoPrendaId)
                .NotEmpty().WithMessage("El tipo de prenda es obligatorio.");

            prenda.RuleFor(p => p.Cantidad)
                .GreaterThanOrEqualTo(1).WithMessage("La cantidad debe ser al menos 1.");

            prenda.RuleFor(p => p.Descripcion)
                .NotEmpty().WithMessage("La descripción del trabajo es obligatoria.")
                .MaximumLength(500).WithMessage("La descripción no puede superar 500 caracteres.");

            prenda.RuleFor(p => p.PrecioPorUnidad)
                .GreaterThan(0).WithMessage("El precio por unidad debe ser mayor a 0.");

            prenda.RuleFor(p => p.FechaEntrega)
                .GreaterThanOrEqualTo(DateOnly.FromDateTime(DateTime.UtcNow))
                .WithMessage("La fecha de entrega no puede ser anterior al día actual.");
        });
    }
}
