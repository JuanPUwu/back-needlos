using FluentValidation;

namespace Needlos.Aplicacion.Servicios.Comandos.CrearServicio;

public class CrearServicioValidator : AbstractValidator<CrearServicioCommand>
{
    public CrearServicioValidator()
    {
        RuleFor(x => x.Nombre)
            .NotEmpty().WithMessage("El nombre del servicio es obligatorio.")
            .MaximumLength(100).WithMessage("El nombre no puede superar 100 caracteres.");

        RuleFor(x => x.PrecioBase)
            .GreaterThan(0).WithMessage("El precio base debe ser mayor a 0.");
    }
}
