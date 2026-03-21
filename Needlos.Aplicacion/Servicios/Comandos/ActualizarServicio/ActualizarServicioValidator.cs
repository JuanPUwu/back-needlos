using FluentValidation;

namespace Needlos.Aplicacion.Servicios.Comandos.ActualizarServicio;

public class ActualizarServicioValidator : AbstractValidator<ActualizarServicioCommand>
{
    public ActualizarServicioValidator()
    {
        RuleFor(x => x.Id)
            .NotEmpty().WithMessage("El id del servicio es obligatorio.");

        RuleFor(x => x.Nombre)
            .NotEmpty().WithMessage("El nombre es obligatorio.")
            .MaximumLength(100).WithMessage("El nombre no puede superar los 100 caracteres.");

        RuleFor(x => x.PrecioBase)
            .GreaterThan(0).WithMessage("El precio base debe ser mayor que cero.");
    }
}
